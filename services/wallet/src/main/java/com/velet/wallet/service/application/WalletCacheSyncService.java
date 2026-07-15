package com.velet.wallet.service.application;

import com.velet.wallet.infrastructure.consumer.wallet.event.BalanceReservationCreatedEvent;
import com.velet.wallet.infrastructure.consumer.wallet.event.TransactionCancelledEvent;
import com.velet.wallet.infrastructure.consumer.wallet.event.TransferCompletedEvent;

public interface WalletCacheSyncService {
    void syncBalance(Long eventId, TransferCompletedEvent payload);
    void reserveBalance(Long eventId, BalanceReservationCreatedEvent payload);
    void releaseBalance(Long eventId, TransactionCancelledEvent payload);
}
