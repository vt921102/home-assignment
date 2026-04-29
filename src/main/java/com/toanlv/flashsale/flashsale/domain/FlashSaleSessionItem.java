package com.toanlv.flashsale.flashsale.domain;

import java.math.BigDecimal;
import java.util.UUID;

import com.toanlv.flashsale.product.domain.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;

@Getter
@Entity
@Table(
    name = "flash_sale_session_items",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_flash_sale_session_product",
            columnNames = {"session_id", "product_id"}),
    indexes = {
      @Index(name = "idx_flash_sale_session_items_session", columnList = "session_id"),
      @Index(name = "idx_flash_sale_session_items_product", columnList = "product_id")
    })
public class FlashSaleSessionItem {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", nullable = false)
  private FlashSaleSession session;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "sale_price", nullable = false, precision = 19, scale = 2)
  private BigDecimal salePrice;

  @Column(name = "total_quantity", nullable = false)
  private int totalQuantity;

  @Column(name = "sold_quantity", nullable = false)
  private int soldQuantity = 0;

  @Column(name = "per_user_limit", nullable = false)
  private int perUserLimit = 1;

  /** Optimistic lock version. Used in decrementSold native UPDATE WHERE version = :version. */
  @Version private Long version = 0L;

  // ----------------------------------------------------------------
  // Factory
  // ----------------------------------------------------------------

  public static FlashSaleSessionItem create(
      FlashSaleSession session,
      Product product,
      BigDecimal salePrice,
      int totalQuantity,
      int perUserLimit) {
    var item = new FlashSaleSessionItem();
    item.session = session;
    item.product = product;
    item.salePrice = salePrice;
    item.totalQuantity = totalQuantity;
    item.soldQuantity = 0;
    item.perUserLimit = perUserLimit;
    return item;
  }

  // ----------------------------------------------------------------
  // Business methods
  // ----------------------------------------------------------------

  public boolean hasStock() {
    return this.soldQuantity < this.totalQuantity;
  }

  public int getRemainingQuantity() {
    return this.totalQuantity - this.soldQuantity;
  }
}
