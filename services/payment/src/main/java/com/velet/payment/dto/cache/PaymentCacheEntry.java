package com.velet.payment.dto.cache;

import lombok.Builder;

@Builder
public record PaymentCacheEntry(
        Long paymentId,
        String status,
        Long finalPrice,
        String cancelledReason,
        String updatedAt
) {}
