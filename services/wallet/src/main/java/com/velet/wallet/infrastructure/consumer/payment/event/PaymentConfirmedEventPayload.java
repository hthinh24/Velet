package com.velet.wallet.infrastructure.consumer.payment.event;

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
        String voucherFundedBy
) {}
