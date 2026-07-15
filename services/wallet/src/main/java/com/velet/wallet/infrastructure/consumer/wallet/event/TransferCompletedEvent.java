package com.velet.wallet.infrastructure.consumer.wallet.event;

import java.math.BigDecimal;

public record TransferCompletedEvent(
    String transactionId,
    Long fromWalletId,
    Long toWalletId,
    BigDecimal amount,
    String currency,
    String occurredAt
) {}