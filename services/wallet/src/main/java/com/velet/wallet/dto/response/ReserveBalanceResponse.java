package com.velet.wallet.dto.response;

import com.velet.wallet.models.enums.ReservationStatus;

public record ReserveBalanceResponse(
    ReservationStatus status
) {}