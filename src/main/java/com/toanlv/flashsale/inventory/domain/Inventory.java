package com.toanlv.flashsale.inventory.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;

@Getter
@Entity
@Table(
    name = "inventories",
    uniqueConstraints =
        @UniqueConstraint(name = "uq_inventories_product", columnNames = "product_id"))
public class Inventory {

  @Id @GeneratedValue private UUID id;

  @Column(name = "product_id", nullable = false, unique = true)
  private UUID productId;

  @Column(name = "total_quantity", nullable = false)
  private int totalQuantity = 0;

  @Column(name = "reserved_quantity", nullable = false)
  private int reservedQuantity = 0;

  @Column(name = "available_quantity", nullable = false)
  private int availableQuantity = 0;

  @Version private Long version;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // ----------------------------------------------------------------
  // Factory methods
  // ----------------------------------------------------------------

  /** Initialize inventory for a newly created product with zero stock. */
  public static Inventory init(UUID productId) {
    var inv = new Inventory();
    inv.productId = productId;
    inv.totalQuantity = 0;
    inv.reservedQuantity = 0;
    inv.availableQuantity = 0;
    return inv;
  }

  /** Initialize inventory with a specified quantity. */
  public static Inventory initWithStock(UUID productId, int quantity) {
    var inv = new Inventory();
    inv.productId = productId;
    inv.totalQuantity = quantity;
    inv.reservedQuantity = 0;
    inv.availableQuantity = quantity;
    return inv;
  }

  // ----------------------------------------------------------------
  // Business methods
  // ----------------------------------------------------------------

  /**
   * Restock — add quantity to total and available.
   *
   * @param quantity amount to add, must be positive
   */
  public void restock(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Restock quantity must be positive, got: " + quantity);
    }
    this.totalQuantity += quantity;
    this.availableQuantity += quantity;
  }

  /**
   * Reserve stock — move from available to reserved. Called when a purchase is confirmed.
   *
   * @param quantity amount to reserve
   */
  public void reserve(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Reserve quantity must be positive, got: " + quantity);
    }
    if (this.availableQuantity < quantity) {
      throw new IllegalStateException(
          "Insufficient available stock. Available: "
              + availableQuantity
              + ", requested: "
              + quantity);
    }
    this.availableQuantity -= quantity;
    this.reservedQuantity += quantity;
  }

  /**
   * Release reserved stock — move from reserved back to available. Called on order refund or
   * cancellation.
   *
   * @param quantity amount to release
   */
  public void release(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Release quantity must be positive, got: " + quantity);
    }
    if (this.reservedQuantity < quantity) {
      throw new IllegalStateException(
          "Cannot release more than reserved. Reserved: "
              + reservedQuantity
              + ", requested: "
              + quantity);
    }
    this.reservedQuantity -= quantity;
    this.availableQuantity += quantity;
  }

  /**
   * Confirm sale — remove from reserved and total. Called after order is fully completed.
   *
   * @param quantity amount to confirm
   */
  public void confirmSale(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Confirm quantity must be positive, got: " + quantity);
    }
    if (this.reservedQuantity < quantity) {
      throw new IllegalStateException(
          "Cannot confirm more than reserved. Reserved: "
              + reservedQuantity
              + ", requested: "
              + quantity);
    }
    this.reservedQuantity -= quantity;
    this.totalQuantity -= quantity;
  }

  /**
   * Verify the inventory invariant: total = available + reserved. Used in reconciliation and tests.
   */
  public boolean isConsistent() {
    return this.totalQuantity == this.availableQuantity + this.reservedQuantity
        && this.availableQuantity >= 0
        && this.reservedQuantity >= 0
        && this.totalQuantity >= 0;
  }
}
