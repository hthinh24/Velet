package com.velet.wallet.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String userId,
        @NotBlank String fromWalletId,
        @NotBlank String toWalletId,
        @NotNull @Positive BigDecimal amount,
        @Nullable Long voucherId,
        @Nullable BigDecimal points,

        // Inject via HTTP Request Header "X-Idempotency-Key"
        @NotBlank String idempotencyKey
) {}