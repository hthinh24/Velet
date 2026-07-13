package com.velet.payment.client.feign;

import com.velet.payment.dto.client.WalletBalanceCheckResponse;
import com.velet.payment.dto.client.WalletReserveRequest;
import com.velet.payment.dto.client.WalletReserveResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.velet.payment.dto.common.ApiResponse;

@FeignClient(name = "wallet-feign-client", url = "${clients.wallet.base-url}")
public interface WalletFeignClient {
    @GetMapping("/api/v1/wallet/{walletId}/balance-check")
    ApiResponse<WalletBalanceCheckResponse> checkBalance(@PathVariable("walletId") Long walletId);

    @PostMapping("/api/v1/wallet/reserve")
    ApiResponse<WalletReserveResponse> reserve(@RequestBody WalletReserveRequest request);
}