package com.velet.wallet.dto.event;

import java.math.BigDecimal;

public record BalanceReservationCreatedEvent(
    String transactionId,
    Long fromWalletId,
    Long toWalletId,
    BigDecimal amount,
    String currency,
    String occurredAt
) {}