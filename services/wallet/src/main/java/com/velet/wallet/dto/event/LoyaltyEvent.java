package com.velet.wallet.dto.event;

import java.math.BigDecimal;

public record LoyaltyEvent(
    String transactionId,
    Long userId,
    Long voucherId,
    BigDecimal points,
    String occurredAt
) {}