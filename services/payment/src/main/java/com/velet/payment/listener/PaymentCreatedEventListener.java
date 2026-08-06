package com.velet.payment.listener;

import com.velet.payment.listener.event.PaymentCreatedEvent;
import com.velet.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCreatedEventListener {

    private final PaymentService paymentService;

    @EventListener
    @Async
    public void onPaymentCreated(PaymentCreatedEvent event) {
        log.info("payment.process.fast-path paymentId={}", event.getPaymentId());
        paymentService.processPayment(event.getPaymentId());
    }
}