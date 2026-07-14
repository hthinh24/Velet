package com.velet.wallet.dto.response;

public record WalletBalanceResponse(
        Long availableBalance,
        String status
) {}
