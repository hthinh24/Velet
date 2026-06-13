package com.velet.wallet.models;

import com.velet.wallet.models.enums.EntryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry extends BaseCreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "entry_type", nullable = false, columnDefinition = "entry_type")
    private EntryType entryType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "running_balance", nullable = false)
    private Long runningBalance;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.UUID)
    private String idempotencyKey;
}
