package com.velet.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ReserveBalanceRequest(
        @NotBlank String walletId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String idempotencyKey
) {}