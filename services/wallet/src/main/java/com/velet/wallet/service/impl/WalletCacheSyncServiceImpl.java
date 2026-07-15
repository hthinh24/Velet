package com.velet.wallet.service.impl;

import com.velet.wallet.infrastructure.consumer.wallet.event.BalanceReservationCreatedEvent;
import com.velet.wallet.infrastructure.consumer.wallet.event.TransactionCancelledEvent;
import com.velet.wallet.infrastructure.consumer.wallet.event.TransferCompletedEvent;
import com.velet.wallet.models.ProcessedEvent;
import com.velet.wallet.models.enums.ProcessingStatus;
import com.velet.wallet.repository.ProcessedEventRepository;
import com.velet.wallet.repository.WalletCacheRepository;
import com.velet.wallet.service.WalletCacheSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletCacheSyncServiceImpl implements WalletCacheSyncService {

    private final ProcessedEventRepository processedEventRepository;
    private final WalletCacheRepository cacheRepo;

    private final TransactionTemplate transactionTemplate;

    private static final String AVAILABLE_BALANCE = "availableBalance";
    private static final String PENDING_BALANCE = "pendingBalance";

    private static final String POSTED_CREDITS = "postedCredits";
    private static final String POSTED_DEBITS = "postedDebits";
    private static final String PENDING_CREDITS = "pendingCredits";
    private static final String PENDING_DEBITS = "pendingDebits";

    @Override
    public void syncBalance(Long eventId, TransferCompletedEvent payload) {
        ProcessedEvent existing = tryClaim(eventId);

        if (existing != null && existing.getStatus() == ProcessingStatus.DONE) {
            log.info("Event {} already fully processed, skip", eventId);
            return;
        }

        // Claim successfully || existing.status == PROCESSING
        applyBalanceDeltaToRedis(payload);

        transactionTemplate.executeWithoutResult(status ->
                                                         processedEventRepository.markDone(eventId, Instant.now()));

        log.info("Transfer event with id: {} processed successfully", eventId);
    }

    @Override
    public void reserveBalance(Long eventId, BalanceReservationCreatedEvent payload) {
        ProcessedEvent existing = tryClaim(eventId);

        if (existing != null && existing.getStatus() == ProcessingStatus.DONE) {
            log.info("Event {} already fully processed, skip", eventId);
            return;
        }

        reserveBalanceOnCache(payload);

        transactionTemplate.executeWithoutResult(status ->
                                                         processedEventRepository.markDone(eventId, Instant.now()));

        log.info("Reserve event with id: {} processed successfully", eventId);
    }

    @Override
    public void releaseBalance(Long eventId, TransactionCancelledEvent payload) {
        ProcessedEvent existing = tryClaim(eventId);

        if (existing != null && existing.getStatus() == ProcessingStatus.DONE) {
            log.info("Event {} already fully processed, skip", eventId);
            return;
        }

        releaseBalanceOnCache(payload);

        transactionTemplate.executeWithoutResult(status ->
                                                         processedEventRepository.markDone(eventId, Instant.now()));

        log.info("Release event with id: {} processed successfully", eventId);
    }


    /**
     * Try to claim the event for processing by inserting a new record into the processed_event table.
     * Return if existed
     */
    private ProcessedEvent tryClaim(Long eventId) {
        return transactionTemplate.execute(status -> {
            try {
                ProcessedEvent claim = ProcessedEvent.builder()
                                                     .eventId(eventId)
                                                     .status(ProcessingStatus.PROCESSING)
                                                     .createdAt(Instant.now())
                                                     .build();
                processedEventRepository.saveAndFlush(claim);
                return null;
            } catch (DataIntegrityViolationException ex) {
                return processedEventRepository.findById(eventId).orElseThrow();
            }
        });
    }

    private void applyBalanceDeltaToRedis(TransferCompletedEvent event) {
        try {
            cacheRepo.incrementCounter(event.fromWalletId().toString(), POSTED_DEBITS, event.amount().longValue());
            cacheRepo.incrementCounter(event.toWalletId().toString(), POSTED_CREDITS, event.amount().longValue());

            cacheRepo.increaseWalletBalance(event.fromWalletId().toString(), AVAILABLE_BALANCE, event.amount().negate());
            cacheRepo.increaseWalletBalance(event.toWalletId().toString(), AVAILABLE_BALANCE, event.amount());
        } catch (Exception e) {
            log.warn("cache.update.failed senderId={} receiverId={} — will self-heal on cache miss",
                     event.fromWalletId(), event.toWalletId(), e);
        }
    }

    private void reserveBalanceOnCache(BalanceReservationCreatedEvent event) {
        try {
            cacheRepo.incrementCounter(event.fromWalletId().toString(), PENDING_DEBITS, event.amount().longValueExact());
            cacheRepo.incrementCounter(event.toWalletId().toString(), PENDING_CREDITS, event.amount().longValueExact());

            cacheRepo.increaseWalletBalance(event.fromWalletId().toString(), AVAILABLE_BALANCE, event.amount().negate());
            cacheRepo.increaseWalletBalance(event.toWalletId().toString(), PENDING_BALANCE, event.amount());
        } catch (Exception e) {
            log.warn("cache.reserve.failed walletId={} — will self-heal on cache miss",
                     event.fromWalletId(), e);
        }
    }

    private void releaseBalanceOnCache(TransactionCancelledEvent payload) {
        try {
            cacheRepo.incrementCounter(payload.fromWalletId(), PENDING_DEBITS, payload.amount().negate().longValueExact());
            cacheRepo.incrementCounter(payload.toWalletId(), PENDING_CREDITS, payload.amount().negate().longValueExact());

            cacheRepo.increaseWalletBalance(payload.fromWalletId(), AVAILABLE_BALANCE, payload.amount());
            cacheRepo.increaseWalletBalance(payload.toWalletId(), PENDING_BALANCE, payload.amount().negate());
        } catch (Exception e) {
            log.warn("cache.release.failed walletId={} — will self-heal on cache miss",
                     payload.fromWalletId(), e);
        }

    }
}