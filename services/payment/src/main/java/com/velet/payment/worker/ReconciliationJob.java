package com.velet.payment.worker;

import com.velet.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationJob {
    private final PaymentService paymentService;

    private static final int BATCH_SIZE = 100;
    private static final long TIMEOUT_MINUTES = 5L;

    @Scheduled(fixedDelay = 30_000)
    public void reconcile() {
        Instant cutoff = Instant.now().minusSeconds(TIMEOUT_MINUTES * 60);
        try {
            paymentService.cancelTimedOutPayments(cutoff, BATCH_SIZE);
        } catch (Exception ex) {
            log.error("Reconciliation job failed", ex);
            // TODO: metríc/alerting
        }
    }
}
