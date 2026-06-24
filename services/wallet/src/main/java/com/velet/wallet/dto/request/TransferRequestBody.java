package com.velet.wallet.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequestBody(
        @NotBlank String fromWalletId,
        @NotBlank String toWalletId,
        @NotNull @Positive BigDecimal amount,
        @Nullable Long voucherId,
        @Nullable BigDecimal points
) {}
