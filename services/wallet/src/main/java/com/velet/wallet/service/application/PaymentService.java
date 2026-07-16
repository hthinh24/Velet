package com.velet.wallet.service.application;

import com.velet.wallet.infrastructure.consumer.payment.event.PaymentConfirmedEventPayload;

public interface PaymentService {
    void confirmPayment(PaymentConfirmedEventPayload event);
}
