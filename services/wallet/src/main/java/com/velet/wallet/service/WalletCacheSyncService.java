package com.velet.wallet.service;

import com.velet.wallet.dto.event.TransferCompletedEvent;

public interface WalletCacheSyncService {
    void syncBalance(Long eventId, TransferCompletedEvent payload);
}
