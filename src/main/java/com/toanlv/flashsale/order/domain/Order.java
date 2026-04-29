package com.toanlv.flashsale.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;

@Entity
@Table(
    name = "orders",
    uniqueConstraints =
        @UniqueConstraint(name = "uq_orders_idempotency_key", columnNames = "idempotency_key"),
    indexes = {
      @Index(name = "idx_orders_user_created", columnList = "user_id, created_at"),
      @Index(name = "idx_orders_status", columnList = "status")
    })
public class Order {

  @Getter @Id @GeneratedValue private UUID id;

  @Getter
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Getter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrderStatus status = OrderStatus.PENDING;

  @Getter
  @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalAmount;

  @Getter
  @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
  private String idempotencyKey;

  @Getter
  @Column(name = "order_type", nullable = false, length = 20)
  private String orderType;

  @OneToMany(
      mappedBy = "order",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<OrderItem> items = new ArrayList<>();

  @Getter @Version private Long version;

  @Getter
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Getter
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // ----------------------------------------------------------------
  // Factory
  // ----------------------------------------------------------------

  public static Order createFlashSaleOrder(
      UUID userId, String idempotencyKey, BigDecimal totalAmount) {
    var order = new Order();
    order.userId = userId;
    order.idempotencyKey = idempotencyKey;
    order.totalAmount = totalAmount;
    order.orderType = "FLASH_SALE";
    order.status = OrderStatus.PENDING;
    return order;
  }

  // ----------------------------------------------------------------
  // Business methods
  // ----------------------------------------------------------------

  public void addItem(OrderItem item) {
    items.add(item);
    item.assignOrder(this);
  }

  public void complete() {
    this.status = OrderStatus.COMPLETED;
  }

  public void fail() {
    this.status = OrderStatus.FAILED;
  }

  public void refund() {
    if (!OrderStatus.COMPLETED.equals(this.status)) {
      throw new IllegalStateException(
          "Only COMPLETED orders can be refunded. " + "Current status: " + this.status);
    }
    this.status = OrderStatus.REFUNDED;
  }

  public boolean isCompleted() {
    return OrderStatus.COMPLETED.equals(this.status);
  }

  public boolean isRefundable() {
    return OrderStatus.COMPLETED.equals(this.status);
  }

  // ----------------------------------------------------------------
  // Getters
  // ----------------------------------------------------------------

  public List<OrderItem> getItems() {
    return Collections.unmodifiableList(items);
  }
}
