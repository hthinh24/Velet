package com.velet.wallet.dto.request;

public record ConfirmReservationRequest(
        String originIdempotencyKey,
        String confirmIdempotencyKey
) {}