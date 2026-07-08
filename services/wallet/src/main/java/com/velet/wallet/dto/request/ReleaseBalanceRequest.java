package com.velet.wallet.dto.request;

import com.velet.wallet.models.enums.CancelReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReleaseBalanceRequest(
        @NotNull CancelReason reason,
        @NotBlank String originIdempotencyKey,
        @NotBlank String releaseIdempotencyKey
) {
}