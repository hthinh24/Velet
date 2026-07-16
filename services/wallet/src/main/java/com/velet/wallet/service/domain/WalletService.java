package com.velet.wallet.service.domain;

import com.velet.wallet.dto.request.ConfirmReservationRequest;
import com.velet.wallet.dto.request.ReleaseBalanceRequest;
import com.velet.wallet.dto.request.ReserveBalanceRequest;
import com.velet.wallet.dto.request.TransferRequest;
import com.velet.wallet.dto.response.*;
import com.velet.wallet.models.BalanceComponents;

public interface WalletService {
    WalletInfo getWalletById(String walletId);
    BalanceComponents getBalanceComponents(String walletId);
    TransferResponse transfer(TransferRequest transferRequest);
    ReserveBalanceResponse reserve(ReserveBalanceRequest reserveBalanceRequest);
    ReserveBalanceResponse getReservationStatus(String idempotencyKey);
    ConfirmReservationResponse confirmReservation(ConfirmReservationRequest request);
    ReleaseBalanceResponse release(ReleaseBalanceRequest releaseBalanceRequest);
    void validateWalletOwner(Long walletOwnerId, Long userId);

    // Internal methods for internal service calls
    WalletBalanceResponse getWalletBalance(Long walletId);
}
