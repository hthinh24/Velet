package com.velet.payment.client.feign;

import com.velet.payment.dto.client.WalletBalanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.velet.payment.dto.common.ApiResponse;

@FeignClient(name = "wallet-feign-client", url = "${clients.wallet.base-url}")
public interface WalletFeignClient {
    @GetMapping("/internal/v1/wallets/{walletId}/balance-check")
    ApiResponse<WalletBalanceResponse> checkBalance(@PathVariable("walletId") Long walletId);
}