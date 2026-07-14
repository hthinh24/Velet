package com.velet.payment.repository;

import com.velet.payment.dto.cache.PaymentCacheEntry;
import com.velet.payment.models.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentCacheRepository {
    void put(Payment payment);
    Optional<PaymentCacheEntry> getById(Long paymentId);
    Optional<PaymentCacheEntry> getByIdempotencyKey(String idempotencyKey);
    void invalidateAll(List<Long> cancelledIds);
}
