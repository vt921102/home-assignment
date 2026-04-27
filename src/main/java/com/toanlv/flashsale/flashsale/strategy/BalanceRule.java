package com.toanlv.flashsale.flashsale.strategy;

import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Rule 3 — User must have sufficient balance.
 *
 * Application-level pre-check before the atomic DB deduction.
 * The DB UPDATE WHERE balance >= price is the authoritative guard —
 * this rule provides an early exit for clearly insufficient balance.
 */
@Component
public final class BalanceRule implements PurchaseEligibilityRule {

    @Override
    public void check(PurchaseContext ctx) {
        if (ctx.userBalance().compareTo(ctx.item().getSalePrice()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    @Override
    public int order() { return 3; }
}
