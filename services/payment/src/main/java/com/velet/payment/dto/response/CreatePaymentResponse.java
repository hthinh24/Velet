package com.velet.payment.dto.response;

import com.velet.payment.models.enums.PaymentStatus;
import lombok.Builder;

@Builder
public record CreatePaymentResponse(
        Long paymentId,
        PaymentStatus status
) {}
