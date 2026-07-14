package com.velet.payment.client;

import com.velet.payment.dto.client.MerchantMdrResponse;

public interface MerchantClient {
    MerchantMdrResponse getMdrRate(Long merchantId);
}
