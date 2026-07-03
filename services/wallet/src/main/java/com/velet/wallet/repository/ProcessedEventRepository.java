package com.velet.wallet.repository;

import com.velet.wallet.models.ProcessedEvent;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    @Modifying
    @Query(value = """
            UPDATE processed_event SET status = 'DONE', completed_at = :completedAt
            WHERE event_id = :eventId
            """, nativeQuery = true)
    void markDone(@Param("eventId") Long eventId, @Param("completedAt") Instant completedAt);

    @Modifying
    @Query("DELETE FROM ProcessedEvent p WHERE p.completedAt < :threshold")
    int deleteCompletedBefore(@Param("threshold") Instant threshold);
}