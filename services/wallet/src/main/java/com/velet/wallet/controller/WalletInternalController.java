package com.velet.wallet.controller;

import com.velet.wallet.dto.common.ApiResponse;
import com.velet.wallet.dto.request.ReleaseBalanceRequest;
import com.velet.wallet.dto.request.ReserveBalanceRequest;
import com.velet.wallet.dto.request.TransferRequest;
import com.velet.wallet.dto.request.TransferRequestBody;
import com.velet.wallet.dto.response.*;
import com.velet.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
