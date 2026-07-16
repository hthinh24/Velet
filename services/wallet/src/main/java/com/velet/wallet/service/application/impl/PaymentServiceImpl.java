package com.velet.wallet.service.application.impl;

import com.velet.wallet.app.SystemAccountCache;
import com.velet.wallet.dto.request.ConfirmReservationRequest;
import com.velet.wallet.infrastructure.consumer.payment.event.PaymentConfirmedEventPayload;
import com.velet.wallet.models.enums.AccountType;
import com.velet.wallet.models.enums.TransactionType;
import com.velet.wallet.service.application.PaymentService;
import com.velet.wallet.service.domain.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final WalletService walletService;
    private final SystemAccountCache systemAccountCache;

    @Override
    @Transactional
    public void confirmPayment(PaymentConfirmedEventPayload event) {
        log.info("Confirming payment for event: {}", event);

        walletService.confirmReservation(
                new ConfirmReservationRequest(event.originIdempotencyKey(), event.confirmIdempotencyKey()));

        chargeMDRFee(event);

        log.info("Payment confirmed for event: {}", event);
    }

    private void chargeMDRFee(PaymentConfirmedEventPayload event) {
        Long revenueAccountId = systemAccountCache.resolve(AccountType.REVENUE_MDR);

        walletService.postInternalEntry(
                event.merchantWalletId(),
                revenueAccountId,
                TransactionType.MDR_FEE,
                event.mdrFee(),
                event.confirmIdempotencyKey() + ":mdrFee"
        );
    }
}
