package com.velet.wallet.dto.cache;

public record BalanceCounter(
        String postedDebits,
        String postedCredits,
        String pendingDebits,
        String pendingCredits
) {}
