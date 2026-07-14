package com.velet.payment.worker;

import com.velet.payment.models.enums.PaymentStatus;
import com.velet.payment.repository.PaymentRepository;
import com.velet.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessingHandler {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    private static final int BATCH_SIZE = 50;
    private static final long TIMEOUT_MINUTES = 5L;

    // TODO: When prevent race condition is needed,
    //  Suggest change to lease pattern with lock_by, locked_until
    //  instead pessimistic lock to avoid hold a long lock
    @Scheduled(fixedDelay = 30_000)
    public void poll() {
        Instant cutoff = Instant.now().minusSeconds(TIMEOUT_MINUTES * 60);
        List<Long> ids =
                paymentRepository.findInProgressIds(PaymentStatus.IN_PROGRESS, cutoff, PageRequest.of(0, BATCH_SIZE));
        if (ids.isEmpty()) return;

        log.info("payment.poller.found count={}", ids.size());
        ids.forEach(paymentService::processPayment);
    }
}
