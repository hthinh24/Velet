package com.velet.wallet.repository;

import com.velet.wallet.models.BalanceSnapshot;
import com.velet.wallet.models.BalanceSnapshot.BalanceSnapshotId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BalanceSnapshotRepository extends JpaRepository<BalanceSnapshot, BalanceSnapshotId> {

    @Query(value = """
            SELECT * FROM balance_snapshots
            WHERE wallet_id = :walletId
            ORDER BY snapshot_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<BalanceSnapshot> findLatestByWalletId(@Param("walletId") Long walletId);

    @Query("SELECT DISTINCT s.id.walletId FROM BalanceSnapshot s")
    List<Long> findAllSnapshotWalletIds();
}
