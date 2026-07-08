package com.velet.wallet.dto.event;

import java.math.BigDecimal;

public record TransactionCanceledEvent(
    String transactionId,
    String fromWalletId,
    String toWalletId,
    BigDecimal amount,
    String occurredAt
) {}