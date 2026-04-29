package com.toanlv.flashsale.inventory.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Getter
@Entity
@Table(
    name = "inventory_audit_logs",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_inventory_audit_source_event",
            columnNames = "source_event_id"),
    indexes = @Index(name = "idx_inventory_audit_product", columnList = "product_id, created_at"))
public class InventoryAuditLog {

  @Id @GeneratedValue private UUID id;

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  /**
   * ID of the outbox event that triggered this change. UNIQUE constraint ensures idempotency — same
   * event cannot produce two audit log entries.
   */
  @Column(name = "source_event_id", nullable = false, unique = true)
  private UUID sourceEventId;

  /** Change in quantity. Negative = decrease, positive = increase. */
  @Column(nullable = false)
  private int delta;

  /** Human-readable reason for the change. */
  @Column(nullable = false, length = 50)
  private String reason;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // ----------------------------------------------------------------
  // Factory
  // ----------------------------------------------------------------

  public static InventoryAuditLog create(
      UUID productId, UUID sourceEventId, int delta, String reason) {
    var log = new InventoryAuditLog();
    log.productId = productId;
    log.sourceEventId = sourceEventId;
    log.delta = delta;
    log.reason = reason;
    return log;
  }
}
