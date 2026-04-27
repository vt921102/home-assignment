package com.toanlv.flashsale.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "Base price must be greater than 0")
        BigDecimal basePrice,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,

        UUID categoryId
) {}
