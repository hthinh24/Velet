package com.velet.payment.client.feign;

import com.velet.payment.dto.client.WalletReserveRequest;
import com.velet.payment.dto.client.WalletReserveResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.velet.payment.dto.common.ApiResponse;

@FeignClient(name = "wallet-feign-client", url = "${clients.wallet.base-url}")
public interface WalletFeignClient {
    @PostMapping("/api/v1/wallets/reserve")
    ApiResponse<WalletReserveResponse> reserve(@RequestBody WalletReserveRequest request);
}