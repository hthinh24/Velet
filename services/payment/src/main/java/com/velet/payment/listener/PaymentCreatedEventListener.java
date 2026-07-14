package com.velet.payment.listener;

import com.velet.payment.listener.event.PaymentCreatedEvent;
import com.velet.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCreatedEventListener {

    private final PaymentService paymentService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onPaymentCreated(PaymentCreatedEvent event) {
        log.info("payment.process.fast-path paymentId={}", event.getPaymentId());
        paymentService.processPayment(event.getPaymentId());
    }
}