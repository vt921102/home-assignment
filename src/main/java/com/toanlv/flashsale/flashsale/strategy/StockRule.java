package com.toanlv.flashsale.flashsale.strategy;


import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Rule 2 — Item must have remaining stock.
 *
 * Application-level pre-check before the atomic DB decrement.
 * The DB UPDATE with version check is the authoritative guard —
 * this rule provides an early exit for clearly out-of-stock items.
 */
@Component
public final class StockRule implements PurchaseEligibilityRule {

    @Override
    public void check(PurchaseContext ctx) {
        if (!ctx.item().hasStock()) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
    }

    @Override
    public int order() { return 2; }
}
