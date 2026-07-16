package com.velet.wallet.service.application.impl;

import com.velet.wallet.dto.cache.BalanceCounter;
import com.velet.wallet.dto.cache.ReservationRecord;
import com.velet.wallet.dto.request.ConfirmReservationRequest;
import com.velet.wallet.dto.request.ReleaseBalanceRequest;
import com.velet.wallet.dto.request.ReserveBalanceRequest;
import com.velet.wallet.dto.request.TransferRequest;
import com.velet.wallet.dto.response.*;
import com.velet.wallet.exception.AppException;
import com.velet.wallet.exception.ErrorCode;
import com.velet.wallet.models.BalanceComponents;
import com.velet.wallet.models.enums.ReservationStatus;
import com.velet.wallet.repository.WalletCacheRepository;
import com.velet.wallet.service.domain.WalletService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates class that interacts with 3rd components
 * such as cache, observations, message queue...
 */
@Service
@Primary
@Slf4j
public class WalletApplicationService implements WalletService {

    private final WalletService walletService;
    private final WalletCacheRepository cacheRepo;
    private final ObservationRegistry observationRegistry;

    public WalletApplicationService(
            @Qualifier("walletDomainService") WalletService walletService,
            WalletCacheRepository cacheRepo,
            ObservationRegistry observationRegistry) {
        this.walletService = walletService;
        this.cacheRepo = cacheRepo;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public WalletInfo getWalletById(String walletId) {
        WalletInfo walletInfo = getWallet(walletId);
        return walletInfo;
    }

    @Override
    public BalanceComponents getBalanceComponents(String walletId) {
        return walletService.getBalanceComponents(walletId);
    }

    @Override
    public WalletBalanceResponse getWalletBalance(Long walletId) {
        return walletService.getWalletBalance(walletId);
    }

    /**
     * transfer flow: validation → distributed locking → Postgres-authoritative balance check →
     * delegates DB writes to WalletServiceExecutor → best-effort cache update → release lock.
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

        // Early-reject: read balance cache to fail-fast on obvious insufficient-fund cases.
        Optional<BalanceComponents> cachedBalance = cacheRepo.getCounters(request.fromWalletId());
        if (cachedBalance.isPresent() &&
            cachedBalance.get().available() < request.amount().longValueExact()) {
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        // Acquire Redis locks smallest-ID-first to prevent deadlock between concurrent transfers.
        String firstLock = request.fromWalletId().compareTo(request.toWalletId()) < 0
                ? request.fromWalletId() : request.toWalletId();
        String secondLock = request.fromWalletId().compareTo(request.toWalletId()) < 0
                ? request.toWalletId() : request.fromWalletId();

        boolean firstAcquire = false;
        boolean secondAcquire = false;

        try {
            acquireOrThrow(firstLock);
            firstAcquire = true;
            acquireOrThrow(secondLock);
            secondAcquire = true;

            log.info("transfer.lock.acquired firstLock={} secondLock={}", firstLock, secondLock);

            long startMs = System.currentTimeMillis();
            TransferResponse response = walletService.transfer(
                    request
            );
            log.info("transfer.completed transactionId={} durationMs={}",
                     response.transactionId(), System.currentTimeMillis() - startMs);

            cacheRepo.releaseLock(firstLock);
            cacheRepo.releaseLock(secondLock);
            return response;

        } catch (AppException e) {
            log.warn("transfer.failed reason={}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("transfer.unexpected.failed reason={}", e.getMessage(), e);
            throw e;
        } finally {
            if (firstAcquire) cacheRepo.releaseLock(firstLock);
            if (secondAcquire) cacheRepo.releaseLock(secondLock);
        }
    }

    @Override
    public ReserveBalanceResponse reserve(ReserveBalanceRequest request) {
        log.info("reserve.started amount={}", request.amount());

        if (request.fromWalletId().equals(request.toWalletId())) {
            throw new AppException(ErrorCode.TRANSFER_TO_SELF);
        }

        Optional<ReservationRecord> existed = cacheRepo.getReservationRecord(request.idempotencyKey());
        if (existed.isPresent() && existed.get().status().equals(ReservationStatus.RESERVED.name())) {
            ReservationRecord reservationRecord = existed.get();
            log.info("reserve.duplicate idempotencyKey={}", request.idempotencyKey());
            return new ReserveBalanceResponse(
                    ReservationStatus.valueOf(reservationRecord.status()),
                    reservationRecord.transactionId(),
                    request.idempotencyKey(),
                    reservationRecord.reservedAt(),
                    reservationRecord.releasedAt()
            );
        }

        WalletInfo sender = getWallet(request.fromWalletId());
        WalletInfo receiver = getWallet(request.toWalletId());

        Optional<BalanceComponents> cachedBalance = cacheRepo.getCounters(request.fromWalletId());
        if (cachedBalance.isPresent() &&
            cachedBalance.get().available() < request.amount().longValueExact()) {
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        String firstLock = request.fromWalletId().compareTo(request.toWalletId()) < 0
                ? request.fromWalletId() : request.toWalletId();
        String secondLock = request.fromWalletId().compareTo(request.toWalletId()) < 0
                ? request.toWalletId() : request.fromWalletId();

        boolean firstAcquire = false;
        boolean secondAcquire = false;

        try {
            acquireOrThrow(firstLock);
            firstAcquire = true;
            acquireOrThrow(secondLock);
            secondAcquire = true;

            log.info("reserve.lock.acquired firstLock={} secondLock={}", firstLock, secondLock);

            long startMs = System.currentTimeMillis();
            ReserveBalanceResponse response = walletService.reserve(request);
            log.info("reserve.completed, durationMs={}",
                     System.currentTimeMillis() - startMs);
            cacheRepo.saveReservationRecord(request.idempotencyKey(),
                                            ReservationRecord.builder()
                                                             .status(response.status().name())
                                                             .transactionId(response.transactionId())
                                                             .walletId(request.fromWalletId())
                                                             .amount(request.amount())
                                                             .reservedAt(response.reservedAt())
                                                             .releasedAt(response.releasedAt())
                                                             .build()
            );

            cacheRepo.releaseLock(firstLock);
            cacheRepo.releaseLock(secondLock);

            return response;

        } catch (AppException e) {
            log.error("reserve.failed reason={}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("reserve.unexpected.failed reason={}", e.getMessage(), e);
            throw e;
        } finally {
            if (firstAcquire) cacheRepo.releaseLock(firstLock);
            if (secondAcquire) cacheRepo.releaseLock(secondLock);
        }
    }

    @Override
    public ConfirmReservationResponse confirmReservation(ConfirmReservationRequest request) {
        return walletService.confirmReservation(request);
    }

    @Override
    public ReserveBalanceResponse getReservationStatus(String idempotencyKey) {
        Optional<ReservationRecord> existing =
                cacheRepo.getReservationRecord(idempotencyKey);

        if (existing.isEmpty()) {
            throw new AppException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        ReservationRecord record = existing.get();
        return new ReserveBalanceResponse(ReservationStatus.valueOf(record.status()), record.transactionId(),
                                          idempotencyKey, record.reservedAt(), record.releasedAt());
    }

    @Override
    public ReleaseBalanceResponse release(ReleaseBalanceRequest request) {
        Optional<ReservationRecord> existed = cacheRepo.getReservationRecord(request.originIdempotencyKey());
        if (existed.isPresent() && existed.get().status().equals(ReservationStatus.RELEASED.name())) {
            ReservationRecord reservationRecord = existed.get();
            log.info("release.duplicate originIdempotencyKey={}", request.originIdempotencyKey());

            return new ReleaseBalanceResponse(
                    ReservationStatus.valueOf(reservationRecord.status()),
                    reservationRecord.transactionId(),
                    request.originIdempotencyKey(),
                    reservationRecord.reservedAt(),
                    reservationRecord.releasedAt()
            );
        }

        ReleaseBalanceResponse release = walletService.release(request);
        cacheRepo.release(release.originIdempotencyKey(), release);

        return release;
    }

    @Override
    public void validateWalletOwner(Long walletOwnerId, Long userId) {
        walletService.validateWalletOwner(walletOwnerId, userId);
    }

    public WalletInfo getWallet(String walletId) {
        return cacheRepo.findAccount(walletId)
                        .orElseGet(() -> loadFromDbAndCache(walletId));
    }

    private WalletInfo loadFromDbAndCache(String walletId) {
        WalletInfo info = walletService.getWalletById(walletId);
        BalanceComponents balance = getBalanceComponents(walletId);

        cacheRepo.saveAccount(walletId, info);
        cacheRepo.saveCounters(walletId, createBalanceCounter(balance));

        return info;
    }

    private BalanceCounter createBalanceCounter(BalanceComponents balance) {
        return new BalanceCounter(
                String.valueOf(balance.postedDebits()),
                String.valueOf(balance.postedCredits()),
                String.valueOf(balance.pendingDebits()),
                String.valueOf(balance.pendingCredits())
        );
    }

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
