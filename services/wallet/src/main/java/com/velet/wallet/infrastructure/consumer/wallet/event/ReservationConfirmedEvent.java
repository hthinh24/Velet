package com.velet.wallet.infrastructure.consumer.wallet.event;

import java.math.BigDecimal;

public record ReservationConfirmedEvent (
        String transactionId,
        String fromWalletId,
        String toWalletId,
        BigDecimal amount,
        String occurredAt
) {}
