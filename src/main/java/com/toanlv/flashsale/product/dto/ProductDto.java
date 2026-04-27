package com.toanlv.flashsale.product.dto;

import com.toanlv.flashsale.product.domain.Product;
import com.toanlv.flashsale.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductDto(
        UUID          id,
        String        sku,
        String        name,
        String        description,
        BigDecimal    basePrice,
        String        imageUrl,
        UUID          categoryId,
        String        categoryName,
        ProductStatus status,
        Instant       createdAt,
        Instant       updatedAt
) {
    public static ProductDto from(Product product) {
        return new ProductDto(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getImageUrl(),
                product.getCategory() != null
                        ? product.getCategory().getId() : null,
                product.getCategory() != null
                        ? product.getCategory().getName() : null,
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
