package com.velet.wallet.service.impl;

import com.velet.wallet.dto.cache.ReservationRecord;
import com.velet.wallet.dto.request.ReleaseBalanceRequest;
import com.velet.wallet.dto.request.ReserveBalanceRequest;
import com.velet.wallet.dto.request.TransferRequest;
import com.velet.wallet.dto.response.ReleaseBalanceResponse;
import com.velet.wallet.dto.response.ReserveBalanceResponse;
import com.velet.wallet.dto.response.WalletInfo;
import com.velet.wallet.dto.response.TransferResponse;
import com.velet.wallet.exception.AppException;
import com.velet.wallet.exception.ErrorCode;
import com.velet.wallet.models.Wallet;
import com.velet.wallet.models.enums.AccountStatus;
import com.velet.wallet.models.enums.ReservationStatus;
import com.velet.wallet.repository.WalletCacheRepository;
import com.velet.wallet.repository.WalletRepository;
import com.velet.wallet.service.WalletService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Orchestrates class that interacts with 3rd components
 * such as cache, observations, message queue...
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletCacheRepository cacheRepo;
    private final WalletServiceExecutor walletServiceExecutor;
    private final ObservationRegistry observationRegistry;

    @Override
    public WalletInfo getWalletById(String userId, String walletId) {
        WalletInfo walletInfo = getWallet(walletId);
        validateWalletOwner(walletInfo.ownerId(), Long.parseLong(userId));
        return walletInfo;
    }

    /**
     * transfer flow: validation → distributed locking → balance check →
     * delegates DB writes to WalletServiceExecutor → release lock.
     */
    @Override
    @Observed(name = "wallet.transfer")
    public TransferResponse transfer(TransferRequest request) {
        Observation observation = observationRegistry.getCurrentObservation();
        if (observation != null) {
            observation.highCardinalityKeyValue("idempotencyKey", request.idempotencyKey());
            observation.highCardinalityKeyValue("fromWalletId", request.fromWalletId());
            observation.highCardinalityKeyValue("toWalletId", request.toWalletId());
            observation.lowCardinalityKeyValue("hasLoyalty",
                                               request.voucherId() != null ||
                                               request.points() != null ? "true" : "false");
        }

        log.info("transfer.started amount={}", request.amount());

        if (request.fromWalletId().equals(request.toWalletId())) {
            throw new AppException(ErrorCode.TRANSFER_TO_SELF);
        }

        WalletInfo sender = getWallet(request.fromWalletId());
        validateWalletOwner(sender.ownerId(), Long.parseLong(request.userId()));

        WalletInfo receiver = getWallet(request.toWalletId());

        // Always acquire smaller walletId lock first to prevent deadlock
        String firstLock = request.fromWalletId().compareTo(request.toWalletId()) < 0
                ? request.fromWalletId() : request.toWalletId();
        String secondLock = request.fromWalletId().compareTo(request.toWalletId()) < 0
                ? request.toWalletId() : request.fromWalletId();

        boolean firstAcquire = false;
        boolean secondAcquire = false;

        boolean isDeducted = false;
        boolean isAdded = false;

        try {
            acquireOrThrow(firstLock);
            firstAcquire = true;

            acquireOrThrow(secondLock);
            secondAcquire = true;

            log.info("transfer.lock.acquired firstLock={} secondLock={}", firstLock, secondLock);

            isDeducted = cacheRepo.deductBalance(sender.walletId(), request.amount().longValueExact());
            isAdded = cacheRepo.addBalance(receiver.walletId(), request.amount().longValueExact());

//            checkSufficientBalance(sender, request.amount());

            long startMs = System.currentTimeMillis();
            TransferResponse response = walletServiceExecutor.transfer(sender.walletId(), receiver.walletId(), request);
            log.info("transfer.completed transactionId={} durationMs={}",
                     response.transactionId(), System.currentTimeMillis() - startMs);

            cacheRepo.releaseLock(firstLock);
            cacheRepo.releaseLock(secondLock);
            return response;

        } catch (AppException e) {
            log.error("transfer.failed reason={}", e.getMessage(), e);

            // compensate cache changes if DB write failed
            try {
                if (isDeducted) cacheRepo.addBalance(sender.walletId(), request.amount().longValueExact());
                if (isAdded) cacheRepo.deductBalance(receiver.walletId(), request.amount().longValueExact());
            } catch (Exception compensateError) {
                log.error("transfer.compensate.FAILED senderId={} receiverId={} " +
                          "originalError={} compensateError={}",
                          sender.walletId(), receiver.walletId(), e.getMessage(), compensateError.getMessage());
                // TODO: Handle compensate FAILED
            }

            if (firstAcquire) cacheRepo.releaseLock(firstLock);
            if (secondAcquire) cacheRepo.releaseLock(secondLock);

            log.error("transfer.failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ReserveBalanceResponse reserve(ReserveBalanceRequest request) {
        getWallet(request.walletId());

        Optional<ReservationRecord> existing =
                cacheRepo.getReservationRecord(request.idempotencyKey());

        if (existing.isPresent()) {
            return new ReserveBalanceResponse(ReservationStatus.RESERVED);
        }

        boolean reserved = cacheRepo.reserve(request.walletId(), request.amount());

        if (!reserved) {
            log.error("reserve failed insufficient funds walletId={} amount={}", request.walletId(), request.amount());
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        cacheRepo.saveReservationRecord(request.idempotencyKey(), ReservationRecord.builder()
                                                                                   .status(ReservationStatus.RESERVED.name())
                                                                                   .walletId(request.walletId())
                                                                                   .amount(request.amount())
                                                                                   .reservedAt(Instant.now().toEpochMilli())
                                                                                   .build());

        return new ReserveBalanceResponse(ReservationStatus.RESERVED);
    }

    @Override
    public ReserveBalanceResponse getReservationStatus(String idempotencyKey) {
        Optional<ReservationRecord> existing =
                cacheRepo.getReservationRecord(idempotencyKey);

        if (existing.isEmpty()) {
            throw new AppException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        return new ReserveBalanceResponse(ReservationStatus.valueOf(existing.get().status()));
    }

    @Override
    public ReleaseBalanceResponse release(ReleaseBalanceRequest request) {
        getWallet(request.walletId());

        Optional<ReservationRecord> existing =
                cacheRepo.getReservationRecord(request.idempotencyKey());

        if (existing.isEmpty()) {
            log.warn("release.not_found idempotencyKey={}", request.idempotencyKey());
            throw new AppException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        if (ReservationStatus.RELEASED.name().equals(existing.get().status())) {
            return new ReleaseBalanceResponse(ReservationStatus.RELEASED);
        }

        cacheRepo.release(request.walletId(), request.amount());
        cacheRepo.updateReservationRecordStatus(request.idempotencyKey(), ReservationStatus.RELEASED.name());

        return new ReleaseBalanceResponse(ReservationStatus.RELEASED);
    }

    private void validateWalletOwner(Long walletOwnerId, Long userId) {
        if (!Objects.equals(walletOwnerId, userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }
    }

    public WalletInfo getWallet(String walletId) {
        return cacheRepo.findAccount(walletId)
                        .orElseGet(() -> loadFromDbAndCache(walletId));
    }

    private WalletInfo loadFromDbAndCache(String walletId) {
        Wallet wallet = walletRepository.findById(Long.parseLong(walletId))
                                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        if (wallet.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.WALLET_INACTIVE);
        }

        WalletInfo info = new WalletInfo(
                wallet.getId(),
                wallet.getOwnerId(),
                wallet.getType(),
                wallet.getAvailableBalance(),
                wallet.getPendingBalance(),
                wallet.getCurrency(),
                wallet.getStatus()
        );
        cacheRepo.saveAccount(walletId, info);
        return info;
    }

//    private void checkSufficientBalance(WalletInfo sender, BigDecimal amount) {
//        long effectiveBalance = sender.availableBalance() - sender.pendingBalance();
//        if (effectiveBalance < amount.longValueExact()) {
//            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
//        }
//    }

    private void acquireOrThrow(String walletId) {
        int[] backoffMs = {100, 200, 500};
        for (int backoff : backoffMs) {
            if (cacheRepo.acquireLock(walletId)) return;
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();

                throw new AppException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }
        }

        log.warn("transfer.lock.failed walletId={} after {} attempts", walletId, backoffMs.length);
        throw new AppException(ErrorCode.LOCK_ACQUISITION_FAILED);
    }
}
