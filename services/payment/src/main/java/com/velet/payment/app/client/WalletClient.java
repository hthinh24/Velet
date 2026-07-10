package com.velet.payment.app.client;

import com.velet.payment.dto.client.WalletBalanceCheckResponse;
import com.velet.payment.dto.client.WalletReserveRequest;
import com.velet.payment.dto.client.WalletReserveResponse;

/**
 * Current implementation: REST over HTTP. Designed so it can be replaced
 * with a gRPC implementation if needed
 */
public interface WalletClient {
    WalletBalanceCheckResponse checkBalance(Long walletId);
    WalletReserveResponse reserve(WalletReserveRequest request);
}
