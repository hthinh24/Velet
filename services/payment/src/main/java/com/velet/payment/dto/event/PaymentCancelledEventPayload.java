package com.velet.payment.dto.event;

import lombok.Builder;

import java.time.Instant;

@Builder
public record PaymentCancelledEventPayload(
        Long aggregateId,
        Long userWalletId,
        Long merchantWalletId,
        String reason,
        Long cancelledAt
) {}
