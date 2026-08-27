package com.velet.payment.dto.event;

import lombok.Builder;

@Builder
public record PaymentConfirmedEventPayload(
        Long aggregateId,
        Long userWalletId,
        Long merchantWalletId,
        Long finalPrice,
        Long merchantNet,
        Long mdrFee,
        Long systemSubsidy,
        Long voucherId,
        Long coinAmount,
        String voucherFundedBy,
        String originIdempotencyKey,
        String confirmIdempotencyKey
) {}
