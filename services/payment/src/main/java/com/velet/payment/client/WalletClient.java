package com.velet.payment.client;

import com.velet.payment.dto.client.WalletBalanceResponse;
import com.velet.payment.dto.client.WalletReserveRequest;
import com.velet.payment.dto.client.WalletReserveResponse;

/**
 * Current implementation: REST over HTTP. Designed so it can be replaced
 * with a gRPC implementation if needed
 */
public interface WalletClient {
    WalletBalanceResponse checkBalance(Long walletId);
    WalletReserveResponse reserve(WalletReserveRequest request);
}
