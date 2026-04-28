package com.toanlv.flashsale.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "order_items",
    indexes = {
      @Index(name = "idx_order_items_order", columnList = "order_id"),
      @Index(name = "idx_order_items_source_ref", columnList = "source_ref_id")
    })
public class OrderItem {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  /**
   * Snapshot of product name at time of purchase. Decoupled from Product entity — future name
   * changes do not affect order history.
   */
  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal subtotal;

  /**
   * Reference to flash_sale_session_items.id for flash sale orders. Nullable — regular orders do
   * not have a source ref.
   */
  @Column(name = "source_ref_id")
  private UUID sourceRefId;

  // ----------------------------------------------------------------
  // Factory
  // ----------------------------------------------------------------

  public static OrderItem of(
      UUID productId, String productName, int quantity, BigDecimal unitPrice, UUID sourceRefId) {
    var item = new OrderItem();
    item.productId = productId;
    item.productName = productName;
    item.quantity = quantity;
    item.unitPrice = unitPrice;
    item.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    item.sourceRefId = sourceRefId;
    return item;
  }

  // ----------------------------------------------------------------
  // Package-private — only Order can assign itself
  // ----------------------------------------------------------------

  void assignOrder(Order order) {
    this.order = order;
  }

  // ----------------------------------------------------------------
  // Getters
  // ----------------------------------------------------------------

  public UUID getId() {
    return id;
  }

  public Order getOrder() {
    return order;
  }

  public UUID getProductId() {
    return productId;
  }

  public String getProductName() {
    return productName;
  }

  public int getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public UUID getSourceRefId() {
    return sourceRefId;
  }
}
