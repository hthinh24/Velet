package com.velet.wallet.dto.response;

import com.velet.wallet.models.enums.AccountStatus;
import com.velet.wallet.models.enums.AccountType;

public record WalletInfo(
        Long walletId,
        Long ownerId,
        AccountType type,
        Long availableBalance,
        Long pendingBalance,
        String currency,
        AccountStatus status
) {}
