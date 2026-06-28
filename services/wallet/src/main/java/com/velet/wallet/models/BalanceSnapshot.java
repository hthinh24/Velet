package com.velet.wallet.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "balance_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceSnapshot {

    @EmbeddedId
    private BalanceSnapshotId id;

    @Column(name = "posted_debits", nullable = false)
    private Long postedDebits;

    @Column(name = "posted_credits", nullable = false)
    private Long postedCredits;

    @Column(name = "pending_debits", nullable = false)
    private Long pendingDebits;

    @Column(name = "pending_credits", nullable = false)
    private Long pendingCredits;

    @Column(name = "last_ledger_entry_id", nullable = false)
    private Long lastLedgerEntryId;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceSnapshotId implements java.io.Serializable {
        @Column(name = "wallet_id")
        private Long walletId;

        @Column(name = "snapshot_at")
        private Instant snapshotAt;
    }
}
