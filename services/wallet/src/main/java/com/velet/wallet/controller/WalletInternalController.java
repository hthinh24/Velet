package com.velet.wallet.controller;

import com.velet.wallet.dto.common.ApiResponse;
import com.velet.wallet.dto.response.*;
import com.velet.wallet.service.domain.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/wallets")
@RequiredArgsConstructor
@Slf4j
public class WalletInternalController {

    private final WalletService walletService;

    @GetMapping("/{walletId}/balance-check")
    public ApiResponse<WalletBalanceResponse> getBalance(
            @PathVariable Long walletId
    ) {
        return ApiResponse.<WalletBalanceResponse>builder()
                .code(200)
                .message("Account info retrieved")
                .data(walletService.getWalletBalance(walletId))
                .build();
    }
}
