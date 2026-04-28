package com.toanlv.flashsale.flashsale.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record PurchaseRequest(
    @NotNull(message = "Session item ID is required") UUID sessionItemId) {}
