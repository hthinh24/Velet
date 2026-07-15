package com.velet.wallet.infrastructure.consumer.payment.event;

import lombok.Builder;

@Builder
public record PaymentCancelledEventPayload(
        Long aggregateId,
        Long userWalletId,
        Long merchantWalletId,
        String reason,
        Long cancelledAt
) {}
