package com.toanlv.flashsale.flashsale.dto;


import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PurchaseRequest(
        @NotNull(message = "Session item ID is required")
        UUID sessionItemId
) {}
