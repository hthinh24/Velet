package com.velet.payment.repository;

import com.velet.payment.models.Payment;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query(value = """
            SELECT id FROM payments
            WHERE status = 'IN_PROGRESS'
            ORDER BY id ASC
            LIMIT :batchSize
            """, nativeQuery = true)
    List<Long> findInProgressIds(@Param("batchSize") int batchSize);
}
