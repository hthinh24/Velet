package com.velet.payment.utils;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MdrCalculator {

    public long computeMdrFee(long originalPrice, BigDecimal mdrRate) {
        return BigDecimal.valueOf(originalPrice)
                         .multiply(mdrRate)
                         .setScale(0, RoundingMode.HALF_UP)
                         .longValueExact();
    }

    public long computeMerchantNet(long originalPrice, long merchantFundedDiscount, long mdrFee) {
        return originalPrice - merchantFundedDiscount - mdrFee;
    }
}
