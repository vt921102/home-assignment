package com.toanlv.flashsale.flashsale.strategy;

import com.toanlv.flashsale.common.exception.BusinessException;

/**
 * Strategy interface for flash sale purchase eligibility rules.
 *
 * <p>Sealed to make the set of rules explicit and discoverable. New rules must be added to the
 * permits clause — intentional design decision to keep rule inventory visible at a glance.
 *
 * <p>Rules are ordered via order() and applied sequentially by PurchaseService. First rule that
 * throws stops evaluation (fail-fast).
 */
public sealed interface PurchaseEligibilityRule
    permits TimeWindowRule, StockRule, BalanceRule, DailyLimitRule {

  /**
   * Check eligibility. Throws BusinessException if rule fails.
   *
   * @param ctx purchase context with all relevant data
   * @throws BusinessException if the user is not eligible
   */
  void check(PurchaseContext ctx) throws BusinessException;

  /** Execution order — lower value runs first. */
  int order();
}
