package com.toanlv.flashsale.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
    @NotBlank(message = "SKU is required")
        @Size(max = 50, message = "SKU must not exceed 50 characters")
        String sku,
    @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,
    @Size(max = 5000, message = "Description must not exceed 5000 characters") String description,
    @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
        BigDecimal basePrice,
    @Size(max = 500, message = "Image URL must not exceed 500 characters") String imageUrl,
    UUID categoryId) {}
