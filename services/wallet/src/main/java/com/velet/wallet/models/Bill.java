package com.velet.wallet.models;

import com.velet.wallet.models.enums.BillStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill extends BaseCreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "provider_code", nullable = false, length = 20)
    private String providerCode;

    @Column(name = "bill_ref_no", nullable = false, length = 100)
    private String billRefNo;

    @Column(name = "period", length = 20)
    private String period;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BillStatus status = BillStatus.UNPAID;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;
}
