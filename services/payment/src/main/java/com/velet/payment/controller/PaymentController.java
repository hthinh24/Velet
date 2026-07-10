package com.velet.payment.controller;

import com.velet.payment.dto.common.ApiResponse;
import com.velet.payment.dto.request.CreatePaymentRequest;
import com.velet.payment.dto.response.CreatePaymentResponse;
import com.velet.payment.dto.response.PaymentStatusResponse;
import com.velet.payment.exception.AppException;
import com.velet.payment.exception.ErrorCode;
import com.velet.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> initiatePayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid CreatePaymentRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        log.info("payment.initiate idempotencyKey={} userWalletId={} amount={}",
                idempotencyKey, request.userWalletId(), request.originalPrice());

        CreatePaymentResponse data = paymentService.initiatePayment(request, idempotencyKey);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.<CreatePaymentResponse>builder()
                        .code(202)
                        .message("Payment is being processed")
                        .data(data)
                        .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.<PaymentStatusResponse>builder()
                .code(200)
                .message("OK")
                .data(paymentService.getById(id))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getByIdempotencyKey(
            @RequestParam String idempotencyKey
    ) {
        return ResponseEntity.ok(ApiResponse.<PaymentStatusResponse>builder()
                .code(200)
                .message("OK")
                .data(paymentService.getByIdempotencyKey(idempotencyKey))
                .build());
    }
}
