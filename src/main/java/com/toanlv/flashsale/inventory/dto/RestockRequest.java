package com.toanlv.flashsale.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RestockRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Restock quantity must be at least 1")
        Integer quantity
) {}
