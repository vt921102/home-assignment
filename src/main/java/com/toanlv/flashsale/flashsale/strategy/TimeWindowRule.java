package com.toanlv.flashsale.flashsale.strategy;


import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Rule 1 — Flash sale must be active and within its time window.
 *
 * Checked first because it is the cheapest check (no DB query)
 * and eliminates requests outside sale hours immediately.
 */
@Component
public final class TimeWindowRule implements PurchaseEligibilityRule {

    @Override
    public void check(PurchaseContext ctx) {
        var session = ctx.item().getSession();
        var today   = LocalDate.now(ctx.clock());
        var now     = LocalTime.now(ctx.clock());

        if (!session.isWithinWindow(today, now)) {
            throw new BusinessException(ErrorCode.FLASH_SALE_NOT_ACTIVE);
        }
    }

    @Override
    public int order() { return 1; }
}
