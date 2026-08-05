package com.velet.wallet.dto.response;

import com.velet.wallet.models.enums.ReservationStatus;

public record ConfirmReservationResponse(
        ReservationStatus status,
        Long transactionId,
        Long fromWalletId,
        Long toWalletId,
        Long amount,
        String originIdempotencyKey,
        Long createdAt,
        Long updatedAt
) {}