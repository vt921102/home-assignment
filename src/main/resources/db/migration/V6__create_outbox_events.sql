-- ============================================================
-- Outbox events
-- Transactional outbox pattern table.
-- Events are written atomically with business data and processed
-- asynchronously by OutboxDispatchWorker.
-- ============================================================
CREATE TABLE outbox_events
(
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    event_type     VARCHAR(50) NOT NULL,
    aggregate_type VARCHAR(30) NOT NULL,
    aggregate_id   UUID        NOT NULL,
    payload        JSONB       NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count    INTEGER     NOT NULL DEFAULT 0,
    next_retry_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    processed_at   TIMESTAMP,

    CONSTRAINT pk_outbox_events
        PRIMARY KEY (id),
    CONSTRAINT chk_outbox_events_status
        CHECK (status IN (
            'PENDING',
            'PROCESSING',
            'COMPLETED',
            'DEAD_LETTER'
        )),
    CONSTRAINT chk_outbox_events_retry_count
        CHECK (retry_count >= 0)
);

CREATE INDEX idx_outbox_pending
    ON outbox_events (status, next_retry_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_aggregate
    ON outbox_events (aggregate_type, aggregate_id);

CREATE INDEX idx_outbox_dead_letter
    ON outbox_events (created_at DESC)
    WHERE status = 'DEAD_LETTER';