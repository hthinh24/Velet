package com.velet.payment.service;

import com.velet.payment.dto.event.PaymentCreatedEventPayload;

import java.time.Instant;

public interface PaymentService {
    void insertAndProcessPayment(PaymentCreatedEventPayload payload);
    void findAndProcessPayment(Long paymentId);
    void cancelTimedOutPayments(Instant cutoff, int batchSize);
}
