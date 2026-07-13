package com.velet.payment.client.impl;

import com.velet.payment.client.WalletClient;
import com.velet.payment.client.feign.WalletFeignClient;
import com.velet.payment.dto.client.WalletBalanceCheckResponse;
import com.velet.payment.dto.client.WalletReserveRequest;
import com.velet.payment.dto.client.WalletReserveResponse;
import com.velet.payment.dto.common.ApiResponse;
import com.velet.payment.exception.AppException;
import com.velet.payment.exception.ErrorCode;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletRestClient implements WalletClient {

    private final WalletFeignClient walletFeignClient;

    @Override
    public WalletBalanceCheckResponse checkBalance(Long walletId) {
        log.debug("wallet.balance-check walletId={}", walletId);
        try {
            ApiResponse<WalletBalanceCheckResponse> response = walletFeignClient.checkBalance(walletId);

            if (response == null || response.getData() == null) {
                throw new AppException(ErrorCode.WALLET_SERVICE_UNAVAILABLE);
            }
            return response.getData();

        } catch (RetryableException ex) {
            log.warn("wallet.balance-check.timeout walletId={}", walletId, ex);
            throw new AppException(ErrorCode.WALLET_SERVICE_UNAVAILABLE);
        } catch (FeignException ex) {
            log.warn("wallet.balance-check.http-error status={} walletId={}", ex.status(), walletId, ex);
            throw new AppException(ErrorCode.WALLET_SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public WalletReserveResponse reserve(WalletReserveRequest request) {
        log.debug("wallet.reserve fromWalletId={} amount={}", request.fromWalletId(), request.amount());
        try {
            ApiResponse<WalletReserveResponse> response = walletFeignClient.reserve(request);

            if (response == null || response.getData() == null) {
                throw new AppException(ErrorCode.WALLET_SERVICE_UNAVAILABLE);
            }
            return response.getData();

        } catch (RetryableException ex) {
            log.warn("wallet.reserve.timeout fromWalletId={}", request.fromWalletId(), ex);
            throw new AppException(ErrorCode.WALLET_SERVICE_UNAVAILABLE);
        } catch (FeignException ex) {
            log.warn("wallet.reserve.http-error status={} fromWalletId={}", ex.status(), request.fromWalletId(), ex);
            throw new AppException(ErrorCode.WALLET_SERVICE_UNAVAILABLE);
        }
    }
}