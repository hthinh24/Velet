package com.velet.wallet.dto.response;

import com.velet.wallet.models.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        String transactionId,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        TransactionStatus status,
        Instant executedAt
) {}