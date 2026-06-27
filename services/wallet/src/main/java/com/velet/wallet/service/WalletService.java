package com.velet.wallet.service;

import com.velet.wallet.dto.request.ReleaseBalanceRequest;
import com.velet.wallet.dto.request.ReserveBalanceRequest;
import com.velet.wallet.dto.request.TransferRequest;
import com.velet.wallet.dto.response.ReleaseBalanceResponse;
import com.velet.wallet.dto.response.ReserveBalanceResponse;
import com.velet.wallet.dto.response.WalletInfo;
import com.velet.wallet.dto.response.TransferResponse;

public interface WalletService {
    WalletInfo getWalletById(String userId, String walletId);
    TransferResponse transfer(TransferRequest transferRequest);
    ReserveBalanceResponse reserve(ReserveBalanceRequest reserveBalanceRequest);
    ReserveBalanceResponse getReservationStatus(String idempotencyKey);
    ReleaseBalanceResponse release(ReleaseBalanceRequest releaseBalanceRequest);
}
