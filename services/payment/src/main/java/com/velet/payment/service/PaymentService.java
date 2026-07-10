package com.velet.payment.service;

import com.velet.payment.dto.request.CreatePaymentRequest;
import com.velet.payment.dto.response.CreatePaymentResponse;
import com.velet.payment.dto.response.PaymentStatusResponse;

public interface PaymentService {
    CreatePaymentResponse initiatePayment(CreatePaymentRequest request, String idempotencyKey);
    PaymentStatusResponse getById(Long id);
    PaymentStatusResponse getByIdempotencyKey(String key);
}
