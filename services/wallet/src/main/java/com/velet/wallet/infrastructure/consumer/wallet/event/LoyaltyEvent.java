package com.velet.wallet.infrastructure.consumer.wallet.event;

import java.math.BigDecimal;

public record LoyaltyEvent(
    String transactionId,
    Long userId,
    Long voucherId,
    BigDecimal points,
    String occurredAt
) {}