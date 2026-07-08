package com.velet.wallet.dto.response;

import com.velet.wallet.models.enums.ReservationStatus;

import java.time.Instant;

public record ReleaseBalanceResponse(
    ReservationStatus status,
    Long transactionId,
    String originIdempotencyKey,
    Long releaseAt
) {}