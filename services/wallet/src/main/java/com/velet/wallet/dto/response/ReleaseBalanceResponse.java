package com.velet.wallet.dto.response;

import com.velet.wallet.models.enums.ReservationStatus;

public record ReleaseBalanceResponse(
    ReservationStatus status,
    Long transactionId,
    String originIdempotencyKey,
    Long reservedAt,
    Long releasedAt
) {}