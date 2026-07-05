package com.velet.wallet.repository;

import com.velet.wallet.dto.cache.BalanceCounter;
import com.velet.wallet.dto.cache.ReservationRecord;
import com.velet.wallet.models.BalanceComponents;
import com.velet.wallet.dto.response.WalletInfo;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletCacheRepository {

    Optional<WalletInfo> findAccount(String walletId);

    void saveAccount(String walletId, WalletInfo wallet);

    void evictAccount(String walletId);

    boolean acquireLock(String walletId);

    void releaseLock(String walletId);

    void increaseWalletBalance(String walletId, String field, BigDecimal amount);

    void incrementCounter(String walletId, String field, long delta);

    Optional<BalanceComponents> getCounters(String walletId);

    void saveCounters(String walletId, BalanceCounter components);

    boolean reserve(String walletId, BigDecimal amount);

    void release(String walletId, BigDecimal amount);

    Optional<ReservationRecord> getReservationRecord(String idempotencyKey);

    void saveReservationRecord(String idempotencyKey, ReservationRecord record);

    void updateReservationRecordStatus(String idempotencyKey, String status);
}
