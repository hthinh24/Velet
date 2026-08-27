package com.velet.payment.dto.client;

public record WalletBalanceResponse(
        Long availableBalance,
        String status
) {}
