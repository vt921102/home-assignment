package com.toanlv.flashsale.product.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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
    name = "product_categories",
    indexes = @Index(name = "idx_product_categories_parent", columnList = "parent_id"))
public class ProductCategory {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private ProductCategory parent;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // ----------------------------------------------------------------
  // Factory
  // ----------------------------------------------------------------

  public static ProductCategory create(String name, ProductCategory parent) {
    var category = new ProductCategory();
    category.name = name;
    category.parent = parent;
    return category;
  }

  // ----------------------------------------------------------------
  // Getters
  // ----------------------------------------------------------------

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public ProductCategory getParent() {
    return parent;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
