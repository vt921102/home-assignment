package com.toanlv.flashsale.flashsale.strategy;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.flashsale.repository.UserDailyPurchaseLimitRepository;

/**
 * Rule 4 — User must not have purchased a flash sale item today.
 *
 * <p>Uses INSERT ON CONFLICT DO NOTHING for atomic enforcement. Returns 0 if the user already has a
 * limit record for today.
 */
@Component
public final class DailyLimitRule implements PurchaseEligibilityRule {

  private final UserDailyPurchaseLimitRepository limitRepository;

  public DailyLimitRule(UserDailyPurchaseLimitRepository limitRepository) {
    this.limitRepository = limitRepository;
  }

  @Override
  public void check(PurchaseContext ctx) {
    var today = LocalDate.now(ctx.clock());
    int inserted = limitRepository.insertIfAbsent(ctx.userId(), today);
    if (inserted == 0) {
      throw new BusinessException(ErrorCode.DAILY_LIMIT_EXCEEDED);
    }
  }

  @Override
  public int order() {
    return 4;
  }
}
