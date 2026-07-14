package com.velet.payment.dto.client;

public record WalletReserveResponse(
        String status,
        Long transactionId,
        String originIdempotencyKey,
        Long reservedAt,
        Long releasedAt
) {}
