package com.toanlv.flashsale.flashsale.strategy;


import com.toanlv.flashsale.flashsale.domain.FlashSaleSessionItem;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

/**
 * Immutable context passed to each PurchaseEligibilityRule.
 * Carries all data needed for eligibility checks without
 * requiring each rule to load data independently.
 */
public record PurchaseContext(
        UUID userId,
        FlashSaleSessionItem item,
        BigDecimal userBalance,
        Clock clock
) {}
