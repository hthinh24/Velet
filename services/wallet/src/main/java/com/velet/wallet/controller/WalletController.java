package com.velet.wallet.controller;

import com.velet.wallet.dto.common.ApiResponse;
import com.velet.wallet.dto.request.TransferRequest;
import com.velet.wallet.dto.request.TransferRequestBody;
import com.velet.wallet.dto.response.TransferResponse;
import com.velet.wallet.dto.response.WalletInfo;
import com.velet.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @RequestBody @Valid TransferRequestBody body,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-User-Id") String userId
    ) {
        var request = new TransferRequest(
                userId,
                body.fromAccount(), body.toAccount(), body.amount(),
                body.voucherId(), body.points(), idempotencyKey
        );
        return ResponseEntity.ok(ApiResponse.<TransferResponse>builder()
                .code(200)
                .message("Transfer successful")
                .data(walletService.transfer(request))
                .build());
    }

    @GetMapping("/{walletId}")
    public ApiResponse<WalletInfo> getBalance(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String walletId
    ) {
        return ApiResponse.<WalletInfo>builder()
                .code(200)
                .message("Account info retrieved")
                .data(walletService.getWalletById(userId, walletId))
                .build();
    }
}
