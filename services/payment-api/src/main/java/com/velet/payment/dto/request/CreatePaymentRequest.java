package com.velet.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentRequest(
        @NotNull(message = "userWalletId is required")
        Long userWalletId,

        @NotNull(message = "merchantWalletId is required")
        Long merchantWalletId,

        @NotNull(message = "originalPrice is required")
        @Positive(message = "originalPrice must be positive")
        Long originalPrice
) {}