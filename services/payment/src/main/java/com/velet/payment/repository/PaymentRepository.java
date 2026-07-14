package com.velet.payment.repository;

import com.velet.payment.models.Payment;
import com.velet.payment.models.enums.PaymentStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query(value = """
            SELECT p.id FROM Payment p
            WHERE p.status =: status
            ORDER BY p.id ASC
            """)
    List<Long> findInProgressIds(
            @Param("status") String status,
            Pageable pageable
    );

    @Query(value = """
            SELECT * FROM payments 
            WHERE status = CAST(:status AS payment_status)
              AND created_at < :cutoff
            ORDER BY id ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Payment> findAndLockTimedOutPayments(
            @Param("status") String status,
            @Param("cutoff") Instant cutoff,
            @Param("batchSize") int batchSize);
}
