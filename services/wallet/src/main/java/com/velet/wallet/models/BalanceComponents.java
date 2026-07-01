package com.velet.wallet.models;

public record BalanceComponents(
        long postedDebits,
        long postedCredits,
        long pendingDebits,
        long pendingCredits
) {
    public long available() {
        return postedCredits - postedDebits - pendingDebits;
    }
}
