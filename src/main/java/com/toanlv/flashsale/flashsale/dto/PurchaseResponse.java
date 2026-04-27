package com.toanlv.flashsale.flashsale.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseResponse(
        UUID       orderId,
        BigDecimal amountPaid,
        BigDecimal newBalance,
        Instant    createdAt
) {}
