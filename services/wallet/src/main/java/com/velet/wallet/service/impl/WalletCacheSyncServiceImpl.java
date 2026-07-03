package com.velet.wallet.service.impl;

import com.velet.wallet.dto.event.TransferCompletedEvent;
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
            cacheRepo.incrementCounter(event.fromWalletId().toString(),   "postedDebits",  event.amount().longValue());
            cacheRepo.incrementCounter(event.toWalletId().toString(), "postedCredits", event.amount().longValue());
        } catch (Exception e) {
            log.warn("cache.update.failed senderId={} receiverId={} — will self-heal on cache miss",
                     event.fromWalletId(), event.toWalletId(), e);
        }
    }
}