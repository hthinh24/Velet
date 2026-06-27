package com.velet.wallet.dto.response;

import com.velet.wallet.models.enums.AccountStatus;
import com.velet.wallet.models.enums.AccountType;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

public record WalletInfo(
        Long walletId,
        Long ownerId,
        AccountType type,
        @JsonSerialize(using = ToStringSerializer.class)
        Long availableBalance,
        @JsonSerialize(using = ToStringSerializer.class)
        Long pendingBalance,
        String currency,
        AccountStatus status
) {
}
