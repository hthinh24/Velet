package com.velet.wallet.repository;

import com.velet.wallet.dto.response.WalletInfo;

import java.util.Optional;

public interface WalletCacheRepository {

    Optional<WalletInfo> findAccount(String walletId);

    void saveAccount(String walletId, WalletInfo wallet);

    void evictAccount(String walletId);

    boolean acquireLock(String walletId);

    void releaseLock(String walletId);
}
