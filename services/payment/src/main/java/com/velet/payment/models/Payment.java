package com.velet.payment.models;

import com.velet.payment.models.enums.PaymentStatus;
import com.velet.payment.models.enums.VoucherFundBy;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.lang.Long;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "payment_status")
    @Builder.Default
    private PaymentStatus status = PaymentStatus.IN_PROGRESS;

    @Column(name = "original_price", nullable = false)
    private Long originalPrice;

    @Column(name = "voucher_id")
    private Long voucherId;

    @Column(name = "voucher_discount", nullable = false)
    @Builder.Default
    private Long voucherDiscount = 0L;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "voucher_funded_by", columnDefinition = "voucher_funded_by")
    private VoucherFundBy voucherFundedBy;

    @Column(name = "coin_amount", nullable = false)
    @Builder.Default
    private Long coinAmount = 0L;

    // MDR snapshot at transaction time, e.g. 0.0150 = 1.5%
    @Column(name = "mdr_rate", precision = 6, scale = 4)
    private BigDecimal mdrRate;

    // mdr_fee = round(originalPrice * mdrRate), rounded with an explicit
    @Column(name = "mdr_fee")
    private Long mdrFee;

    // Final computed amounts — only populated after the price computation step
    @Column(name = "final_price")
    private Long finalPrice; // amount actually charged to the user

    // merchantNet must be derived by subtraction
    // (originalPrice - merchantFundedDiscount - mdrFee),
    // not computed and rounded independently
    @Column(name = "merchant_net")
    private Long merchantNet; // amount credited to the merchant

    @Column(name = "system_subsidy")
    @Builder.Default
    private Long systemSubsidy = 0L; // amount covered by the platform

    @Column(name = "completed_at")
    private Instant completedAt;
}