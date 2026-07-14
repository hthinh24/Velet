package com.velet.payment.dto.client;

import java.math.BigDecimal;

public record WalletReserveRequest(
        String fromWalletId,
        String toWalletId,
        BigDecimal amount,
        String idempotencyKey
) {}
