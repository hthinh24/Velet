package com.velet.payment.client.feign;

import com.velet.payment.dto.client.MerchantMdrResponse;
import com.velet.payment.dto.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "merchant-feign-client", url = "${clients.merchant.base-url}")
public interface MerchantFeignClient {
    @GetMapping("/api/v1/merchants/{merchantId}/mdr-rate")
    ApiResponse<MerchantMdrResponse> getMdrRate(@PathVariable("merchantId") Long merchantId);
}