package com.velet.wallet.service;

import com.velet.wallet.dto.request.TransferRequest;
import com.velet.wallet.dto.response.WalletInfo;
import com.velet.wallet.dto.response.TransferResponse;

public interface WalletService {
    WalletInfo getWalletById(String userId, String walletId);
    TransferResponse transfer(TransferRequest transferRequest);
}
