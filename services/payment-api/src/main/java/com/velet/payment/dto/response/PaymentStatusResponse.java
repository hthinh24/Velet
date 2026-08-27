package com.velet.payment.dto.response;

import com.velet.payment.models.enums.PaymentStatus;
import lombok.Builder;

@Builder
public record PaymentStatusResponse(
        Long paymentId,
        PaymentStatus status,
        Long finalPrice,
        String cancelledReason
) {}
