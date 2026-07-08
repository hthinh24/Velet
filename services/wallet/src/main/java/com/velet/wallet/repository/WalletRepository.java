package com.velet.wallet.repository;

import com.velet.wallet.models.Wallet;

import com.velet.wallet.models.enums.AccountType;
import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    @Query("SELECT a FROM Wallet a WHERE a.ownerId = :ownerId")
    List<Wallet> findAllByOwnerId(String ownerId);

    @Query("SELECT w FROM Wallet w WHERE w.type IN :types")
    List<Wallet> findByAccountTypeIn(@Param("types") Collection<AccountType> types);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :walletId")
    Optional<Wallet> findByIdWithLock(@Param("walletId") Long walletId);

    /**
     * Computes balance from latest snapshot + delta ledger entries.
     * Avoids full-table scan: only reads entries newer than the snapshot checkpoint.
     * For wallets with no snapshot yet, reads full ledger history (acceptable for new wallets).
     */
    @Query(value = """
            WITH latest_snapshot AS (
                SELECT posted_debits, posted_credits, pending_debits, pending_credits, last_ledger_entry_id
                FROM balance_snapshots
                WHERE wallet_id = :walletId
                ORDER BY snapshot_at DESC
                LIMIT 1
            ),
            delta AS (
                SELECT
                    COALESCE(SUM(CASE WHEN entry_type = 'DEBIT'  AND status = 'POSTED'   THEN amount ELSE 0 END), 0) AS d_posted_debits,
                    COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' AND status = 'POSTED'   THEN amount ELSE 0 END), 0) AS d_posted_credits,
                    COALESCE(SUM(CASE WHEN entry_type = 'DEBIT'  AND status = 'PENDING'  THEN amount ELSE 0 END), 0) AS d_pending_debits,
                    COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' AND status = 'PENDING'  THEN amount ELSE 0 END), 0) AS d_pending_credits
                FROM ledger_entries
                WHERE wallet_id = :walletId
                  AND id > COALESCE((SELECT last_ledger_entry_id FROM latest_snapshot), 0)
            )
            SELECT
                COALESCE((SELECT posted_debits  FROM latest_snapshot), 0) + d.d_posted_debits   AS postedDebits,
                COALESCE((SELECT posted_credits FROM latest_snapshot), 0) + d.d_posted_credits  AS postedCredits,
                COALESCE((SELECT pending_debits FROM latest_snapshot), 0) + d.d_pending_debits  AS pendingDebits,
                COALESCE((SELECT pending_credits FROM latest_snapshot), 0) + d.d_pending_credits AS pendingCredits
            FROM delta d
            """, nativeQuery = true)
    BalanceRow computeBalanceRaw(@Param("walletId") Long walletId);

    interface BalanceRow {
        long getPostedDebits();
        long getPostedCredits();
        long getPendingDebits();
        long getPendingCredits();
    }
}
