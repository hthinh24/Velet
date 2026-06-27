package com.velet.wallet.repository;

import com.velet.wallet.dto.cache.ReservationRecord;
import com.velet.wallet.dto.response.WalletInfo;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletCacheRepository {

    Optional<WalletInfo> findAccount(String walletId);

    void saveAccount(String walletId, WalletInfo wallet);

    void evictAccount(String walletId);

    boolean deductBalance(Long walletId, Long amount);

    boolean addBalance(Long walletId, Long amount);

    boolean acquireLock(String walletId);

    void releaseLock(String walletId);

    boolean reserve(String walletId, BigDecimal amount);

    void release(String walletId, BigDecimal amount);

    Optional<ReservationRecord> getReservationRecord(String idempotencyKey);

    void saveReservationRecord(String idempotencyKey, ReservationRecord record);

    void updateReservationRecordStatus(String idempotencyKey, String status);
}
