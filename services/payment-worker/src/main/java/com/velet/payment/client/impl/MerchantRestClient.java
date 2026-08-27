package com.velet.payment.client.impl;

import com.velet.payment.client.MerchantClient;
import com.velet.payment.client.feign.MerchantFeignClient;
import com.velet.payment.dto.client.MerchantMdrResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class MerchantRestClient implements MerchantClient {

    private final MerchantFeignClient merchantFeignClient;

    // Mock result
    private static final BigDecimal FLAT_MDR_RATE = new BigDecimal("0.0150");

    @Override
    public MerchantMdrResponse getMdrRate(Long merchantId) {
        log.debug("merchant.mdr-rate.mock merchantId={} rate={}", merchantId, FLAT_MDR_RATE);

        // TODO: replace with real REST call when Merchant Service is available
        return new MerchantMdrResponse(merchantId, FLAT_MDR_RATE);
    }

//    @Override
//    public MerchantMdrResponse getMdrRate(Long merchantId) {
//        log.debug("merchant.get-mdr-rate merchantId={}", merchantId);
//        try {
//            ApiResponse<MerchantMdrResponse> response = merchantFeignClient.getMdrRate(merchantId);
//
//            if (response == null || response.getData() == null) {
//                throw new AppException(ErrorCode.MERCHANT_SERVICE_UNAVAILABLE);
//            }
//            return response.getData();
//
//        } catch (FeignException ex) {
//            if (ex instanceof feign.RetryableException || ex.status() == -1) {
//                log.warn("merchant.mdr-rate.timeout or network error merchantId={}", merchantId, ex);
//            } else {
//                log.warn("merchant.mdr-rate.http-error status={} merchantId={}", ex.status(), merchantId, ex);
//            }
//            throw new AppException(ErrorCode.MERCHANT_SERVICE_UNAVAILABLE);
//        }
//    }
}
