package com.velet.payment.dto.client;

import java.math.BigDecimal;

public record MerchantMdrResponse(
        Long merchantId,
        BigDecimal mdrRate   // e.g. 0.0150 for 1.5%
) {}
