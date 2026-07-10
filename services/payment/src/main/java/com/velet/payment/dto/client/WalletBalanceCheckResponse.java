package com.velet.payment.dto.client;

public record WalletBalanceCheckResponse(
        Long availableBalance,
        String status
) {}
