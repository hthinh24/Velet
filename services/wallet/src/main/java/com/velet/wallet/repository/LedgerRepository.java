package com.velet.wallet.repository;

import com.velet.wallet.models.LedgerEntry;
import com.velet.wallet.models.enums.LedgerEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT MAX(l.id) FROM LedgerEntry l WHERE l.wallet.id = :walletId")
    Long findMaxIdByWalletId(@Param("walletId") Long walletId);

    @Query("SELECT DISTINCT l.wallet.id FROM LedgerEntry l")
    List<Long> findAllActiveWalletIds();

    @Query("SELECT l FROM LedgerEntry l WHERE l.idempotencyKey = :idempotencyKey")
    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT l FROM LedgerEntry l WHERE l.idempotencyKey IN :idempotencyKeys")
    List<LedgerEntry> findByIdempotencyKeys(List<String> idempotencyKeys);

    @Modifying
    @Query("UPDATE LedgerEntry l SET l.status = :ledgerEntryStatus WHERE l.id = :id")
    void updateLedgerEntryStatus(Long id, LedgerEntryStatus ledgerEntryStatus);
}

