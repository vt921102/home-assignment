package com.toanlv.flashsale.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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
    name = "balance_transactions",
    indexes = {
      @Index(name = "idx_balance_transactions_user", columnList = "user_id, created_at"),
      @Index(name = "idx_balance_transactions_order", columnList = "order_id")
    })
public class BalanceTransaction {

  public enum Direction {
    DEBIT,
    CREDIT
  }

  @Id @GeneratedValue private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "order_id")
  private UUID orderId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Direction direction;

  @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
  private BigDecimal balanceAfter;

  @Column(nullable = false, length = 50)
  private String reason;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // ----------------------------------------------------------------
  // Factory methods
  // ----------------------------------------------------------------

  public static BalanceTransaction debit(
      UUID userId, UUID orderId, BigDecimal amount, BigDecimal balanceAfter, String reason) {
    var tx = new BalanceTransaction();
    tx.userId = userId;
    tx.orderId = orderId;
    tx.amount = amount;
    tx.direction = Direction.DEBIT;
    tx.balanceAfter = balanceAfter;
    tx.reason = reason;
    return tx;
  }

  public static BalanceTransaction credit(
      UUID userId, UUID orderId, BigDecimal amount, BigDecimal balanceAfter, String reason) {
    var tx = new BalanceTransaction();
    tx.userId = userId;
    tx.orderId = orderId;
    tx.amount = amount;
    tx.direction = Direction.CREDIT;
    tx.balanceAfter = balanceAfter;
    tx.reason = reason;
    return tx;
  }
}
