package com.velet.wallet.controller;

import com.velet.wallet.dto.common.ApiResponse;
import com.velet.wallet.dto.request.ReleaseBalanceRequest;
import com.velet.wallet.dto.request.ReserveBalanceRequest;
import com.velet.wallet.dto.request.TransferRequest;
import com.velet.wallet.dto.request.TransferRequestBody;
import com.velet.wallet.dto.response.ReleaseBalanceResponse;
import com.velet.wallet.dto.response.ReserveBalanceResponse;
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
                body.fromWalletId(), body.toWalletId(), body.amount(),
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

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<ReserveBalanceResponse>> transfer(
            @RequestBody @Valid ReserveBalanceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.<ReserveBalanceResponse>builder()
                .code(200)
                .message("Reserve successful")
                .data(walletService.reserve(request))
                .build());
    }

    @GetMapping("/reserve/{idempotencyKey}")
    public ResponseEntity<ApiResponse<ReserveBalanceResponse>> getReserve(
            @PathVariable String idempotencyKey
    ) {
        return ResponseEntity.ok(ApiResponse.<ReserveBalanceResponse>builder()
                                            .code(200)
                                            .message("Reserve retrieved")
                                            .data(walletService.getReservationStatus(idempotencyKey))
                                            .build());
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<ReleaseBalanceResponse>> transfer(
            @RequestBody @Valid ReleaseBalanceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.<ReleaseBalanceResponse>builder()
                                            .code(200)
                                            .message("Reserve successful")
                                            .data(walletService.release(request))
                                            .build());
    }
}
