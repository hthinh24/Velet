package com.velet.wallet.dto.cache;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record ReservationRecord(
    String status,
    Long transactionId,
    String walletId,
    BigDecimal amount,
    Long reservedAt,
    Long releasedAt
) {}