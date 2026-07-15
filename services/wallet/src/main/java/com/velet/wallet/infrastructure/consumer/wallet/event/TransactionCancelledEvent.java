package com.velet.wallet.infrastructure.consumer.wallet.event;

import java.math.BigDecimal;

public record TransactionCancelledEvent(
    String transactionId,
    String fromWalletId,
    String toWalletId,
    BigDecimal amount,
    String occurredAt
) {}