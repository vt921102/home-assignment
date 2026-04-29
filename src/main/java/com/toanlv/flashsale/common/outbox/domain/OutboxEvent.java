package com.toanlv.flashsale.common.outbox.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(
    name = "outbox_events",
    indexes = {
      @Index(name = "idx_outbox_status_retry", columnList = "status, next_retry_at"),
      @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id")
    })
public class OutboxEvent {

  @Id @GeneratedValue private UUID id;

  /** Business event name. Examples: "OTP_DISPATCH", "FLASH_SALE_PURCHASED", "PRODUCT_RESTOCKED" */
  @Column(name = "event_type", nullable = false, length = 50)
  private String eventType;

  /** Domain entity type that owns this event. Examples: "USER", "ORDER", "PRODUCT" */
  @Column(name = "aggregate_type", nullable = false, length = 30)
  private String aggregateType;

  /** Primary key of the aggregate entity. */
  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  /**
   * Event-specific data as JSONB. Each handler knows the expected payload shape for its event_type.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OutboxStatus status = OutboxStatus.PENDING;

  @Column(name = "retry_count", nullable = false)
  private int retryCount = 0;

  /**
   * Earliest time this event can be retried. Set to NOW() on creation and updated after each failed
   * attempt using exponential backoff.
   */
  @Column(name = "next_retry_at", nullable = false)
  private Instant nextRetryAt = Instant.now();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  // ----------------------------------------------------------------
  // Factory method
  // ----------------------------------------------------------------

  public static OutboxEvent create(
      String eventType, String aggregateType, UUID aggregateId, Map<String, Object> payload) {
    var event = new OutboxEvent();
    event.eventType = eventType;
    event.aggregateType = aggregateType;
    event.aggregateId = aggregateId;
    event.payload = payload;
    return event;
  }

  // ----------------------------------------------------------------
  // Business methods
  // ----------------------------------------------------------------

  /** Mark event as successfully processed. */
  public void markCompleted() {
    this.status = OutboxStatus.COMPLETED;
    this.processedAt = Instant.now();
  }

  /** Mark event as dead letter after exhausting all retries. */
  public void markDeadLetter() {
    this.status = OutboxStatus.DEAD_LETTER;
    this.processedAt = Instant.now();
  }

  /**
   * Schedule next retry using exponential backoff with a cap of 10 minutes. Backoff: 2^retryCount
   * seconds (2s, 4s, 8s, 16s, 32s → capped at 600s)
   */
  public void scheduleRetry() {
    this.retryCount++;
    long delaySec = Math.min((long) Math.pow(2, this.retryCount), 600L);
    this.nextRetryAt = Instant.now().plusSeconds(delaySec);
  }

  /**
   * @return true if max retry attempts have been exhausted
   */
  public boolean isExhausted(int maxAttempts) {
    return this.retryCount >= maxAttempts;
  }
}
