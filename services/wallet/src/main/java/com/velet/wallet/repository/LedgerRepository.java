package com.velet.wallet.repository;

import com.velet.wallet.models.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT MAX(l.id) FROM LedgerEntry l WHERE l.wallet.id = :walletId")
    Long findMaxIdByWalletId(@Param("walletId") Long walletId);

    @Query("SELECT DISTINCT l.wallet.id FROM LedgerEntry l")
    List<Long> findAllActiveWalletIds();
}

