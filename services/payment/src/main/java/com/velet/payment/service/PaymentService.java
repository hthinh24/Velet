package com.velet.payment.service;

import com.velet.payment.dto.request.CreatePaymentRequest;
import com.velet.payment.dto.response.CreatePaymentResponse;
import com.velet.payment.dto.response.PaymentStatusResponse;

import java.time.Instant;
import java.util.List;

public interface PaymentService {
    CreatePaymentResponse initiatePayment(CreatePaymentRequest request, String idempotencyKey);
    PaymentStatusResponse getById(Long id);
    PaymentStatusResponse getByIdempotencyKey(String key);
    void processPayment(Long paymentId);
    void cancelTimedOutPayments(Instant cutoff, int batchSize);
}
