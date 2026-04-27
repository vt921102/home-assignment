package com.toanlv.flashsale.product.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(
                        name = "idx_products_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_products_category",
                        columnList = "category_id"
                )
        }
)
public class Product {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ----------------------------------------------------------------
    // Factory
    // ----------------------------------------------------------------

    public static Product create(
            String sku,
            String name,
            String description,
            BigDecimal basePrice,
            String imageUrl,
            ProductCategory category) {
        var product = new Product();
        product.sku         = sku;
        product.name        = name;
        product.description = description;
        product.basePrice   = basePrice;
        product.imageUrl    = imageUrl;
        product.category    = category;
        product.status      = ProductStatus.ACTIVE;
        return product;
    }

    // ----------------------------------------------------------------
    // Business methods
    // ----------------------------------------------------------------

    public void update(
            String name,
            String description,
            BigDecimal basePrice,
            String imageUrl,
            ProductCategory category) {
        this.name        = name;
        this.description = description;
        this.basePrice   = basePrice;
        this.imageUrl    = imageUrl;
        this.category    = category;
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return ProductStatus.ACTIVE.equals(this.status);
    }

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------

    public UUID getId()                  { return id;          }
    public String getSku()               { return sku;         }
    public String getName()              { return name;        }
    public String getDescription()       { return description; }
    public BigDecimal getBasePrice()     { return basePrice;   }
    public String getImageUrl()          { return imageUrl;    }
    public ProductCategory getCategory() { return category;    }
    public ProductStatus getStatus()     { return status;      }
    public Long getVersion()             { return version;     }
    public Instant getCreatedAt()        { return createdAt;   }
    public Instant getUpdatedAt()        { return updatedAt;   }
}
