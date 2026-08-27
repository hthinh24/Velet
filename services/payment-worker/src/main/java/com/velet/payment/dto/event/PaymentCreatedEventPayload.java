package com.velet.payment.dto.event;

import lombok.Builder;

@Builder
public record PaymentCreatedEventPayload(
        String idempotencyKey,
        Long userWalletId,
        Long merchantWalletId,
        Long originalPrice,
        String traceParent
) {}
