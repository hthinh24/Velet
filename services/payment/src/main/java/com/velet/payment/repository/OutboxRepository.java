package com.velet.payment.repository;

import com.velet.payment.models.Outbox;
import com.velet.payment.models.enums.OutboxStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    @Query("SELECT o FROM Outbox o WHERE o.status = :status")
    List<Outbox> findAllByStatus(OutboxStatus status);

    /**
     * Claims a batch of PENDING outbox rows for publishing.
     * FOR UPDATE SKIP LOCKED prevents duplicate publishing across multiple instances.
     */
    @Query(value = """
            SELECT * FROM outbox
            WHERE status = 'PENDING'
            ORDER BY id ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Outbox> findPendingBatchForUpdate(@Param("batchSize") int batchSize);

    @Modifying
    @Query("UPDATE Outbox o SET o.status = 'PROCESSING' WHERE o.id IN :ids")
    void markAsProcessing(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE Outbox o SET o.status = 'SENT', o.sentAt = :sentAt WHERE o.id IN :ids")
    void markAsSent(@Param("ids") List<Long> ids, @Param("sentAt") Instant sentAt);

    @Modifying
    @Query("UPDATE Outbox o SET o.status = 'PENDING', o.retryCount = o.retryCount + 1 WHERE o.id IN :ids")
    void markAsPendingWithRetry(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE Outbox o SET o.status = 'FAILED', o.retryCount = o.retryCount + 1 WHERE o.id IN :ids")
    void markAsFailed(@Param("ids") List<Long> ids);
}
