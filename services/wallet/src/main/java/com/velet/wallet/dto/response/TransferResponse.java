package com.velet.wallet.dto.response;

import com.velet.wallet.models.enums.TransactionStatus;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        String transactionId,
        String fromWalletId,
        String toWalletId,
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal amount,
        TransactionStatus status,
        Instant executedAt
) {}