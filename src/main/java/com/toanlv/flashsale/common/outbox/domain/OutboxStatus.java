package com.toanlv.flashsale.common.outbox.domain;

/**
 * Lifecycle states of an outbox event.
 *
 * PENDING     — created, waiting to be processed
 * PROCESSING  — currently being processed by a worker (reserved for future use)
 * COMPLETED   — successfully processed
 * DEAD_LETTER — failed after max retry attempts, requires manual intervention
 */
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    DEAD_LETTER
}
