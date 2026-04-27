package com.toanlv.flashsale.common.outbox.repository;


import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;
import com.toanlv.flashsale.common.outbox.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Fetch a batch of pending events that are ready to be processed,
     * skipping any rows locked by another worker instance.
     *
     * FOR UPDATE SKIP LOCKED ensures:
     *   - Only one worker processes each row
     *   - Workers do not block each other in multi-worker scenarios
     *   - Compatible with the LeaderLock pattern (single worker) and
     *     future multi-worker scale-out (skip locked handles distribution)
     *
     * next_retry_at <= NOW() filters out events scheduled for future retry.
     */
    @Query(value = """
            SELECT *
              FROM outbox_events
             WHERE status = 'PENDING'
               AND next_retry_at <= NOW()
             ORDER BY created_at ASC
             LIMIT :limit
               FOR UPDATE SKIP LOCKED
            """,
            nativeQuery = true)
    List<OutboxEvent> fetchPendingBatch(@Param("limit") int limit);

    /**
     * Fetch dead letter events for monitoring or manual reprocessing.
     */
    @Query(value = """
            SELECT *
              FROM outbox_events
             WHERE status = 'DEAD_LETTER'
             ORDER BY created_at DESC
             LIMIT :limit
            """,
            nativeQuery = true)
    List<OutboxEvent> fetchDeadLetterBatch(@Param("limit") int limit);

    /**
     * Count events by status — used by monitoring/actuator.
     */
    long countByStatus(OutboxStatus status);

    /**
     * Find events by aggregate for debugging.
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateId(
            String aggregateType,
            UUID aggregateId);
}
