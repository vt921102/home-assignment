package com.toanlv.flashsale.flashsale.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AddSessionItemRequest(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Sale price is required")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "Sale price must be greater than 0")
        BigDecimal salePrice,

        @NotNull(message = "Total quantity is required")
        @Min(value = 1, message = "Total quantity must be at least 1")
        Integer totalQuantity,

        @Min(value = 1, message = "Per user limit must be at least 1")
        Integer perUserLimit
) {
    public AddSessionItemRequest {
        if (perUserLimit == null) perUserLimit = 1;
    }
}
