package com.toanlv.flashsale.flashsale.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.toanlv.flashsale.auth.repository.UserRepository;
import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.common.outbox.service.IOutboxPublisher;
import com.toanlv.flashsale.flashsale.dto.PurchaseResponse;
import com.toanlv.flashsale.flashsale.repository.FlashSaleSessionItemRepository;
import com.toanlv.flashsale.flashsale.service.IIdempotencyService;
import com.toanlv.flashsale.flashsale.service.IPurchaseService;
import com.toanlv.flashsale.flashsale.strategy.BalanceRule;
import com.toanlv.flashsale.flashsale.strategy.DailyLimitRule;
import com.toanlv.flashsale.flashsale.strategy.PurchaseContext;
import com.toanlv.flashsale.flashsale.strategy.PurchaseEligibilityRule;
import com.toanlv.flashsale.flashsale.strategy.StockRule;
import com.toanlv.flashsale.flashsale.strategy.TimeWindowRule;
import com.toanlv.flashsale.order.service.IOrderService;

@Service
public class PurchaseService implements IPurchaseService {

  private static final Logger log = LoggerFactory.getLogger(PurchaseService.class);

  private final FlashSaleSessionItemRepository itemRepository;
  private final UserRepository userRepository;
  private final IOrderService orderService;
  private final IOutboxPublisher outboxPublisher;
  private final IIdempotencyService idempotencyService;
  private final List<PurchaseEligibilityRule> rules;
  private final Clock clock;
  private final RetryTemplate retryTemplate;

  public PurchaseService(
      FlashSaleSessionItemRepository itemRepository,
      UserRepository userRepository,
      IOrderService orderService,
      IOutboxPublisher outboxPublisher,
      IIdempotencyService idempotencyService,
      TimeWindowRule timeWindowRule,
      StockRule stockRule,
      BalanceRule balanceRule,
      DailyLimitRule dailyLimitRule,
      Clock clock) {
    this.itemRepository = itemRepository;
    this.userRepository = userRepository;
    this.orderService = orderService;
    this.outboxPublisher = outboxPublisher;
    this.idempotencyService = idempotencyService;
    this.clock = clock;

    // Sort rules by order() to guarantee evaluation sequence
    this.rules =
        List.of(timeWindowRule, stockRule, balanceRule, dailyLimitRule).stream()
            .sorted(Comparator.comparingInt(PurchaseEligibilityRule::order))
            .toList();

    // Retry only on optimistic lock conflict — transient error
    this.retryTemplate =
        RetryTemplate.builder()
            .maxAttempts(3)
            .exponentialBackoff(
                Duration.ofMillis(50),
                1.5,
                Duration.ofMillis(500),
                true // jitter prevents thundering herd
                )
            .retryOn(OptimisticLockingFailureException.class)
            .build();
  }

  /**
   * Entry point for flash sale purchase.
   *
   * <p>Pre-transaction: 1. Idempotency cache lookup — return cached response on hit
   *
   * <p>Transactional (with retry on optimistic lock conflict): 2. Load session item with
   * PESSIMISTIC_READ lock 3. Load user balance 4. Run eligibility rules (TimeWindow, Stock,
   * Balance, DailyLimit) 5. Atomic stock decrement (optimistic lock) 6. Atomic balance deduction
   * (WHERE balance >= price) 7. Create order + balance transaction 8. Publish FLASH_SALE_PURCHASED
   * outbox event
   *
   * <p>Post-commit: 9. Cache response in Redis (afterCommit hook)
   */
  @Override
  public PurchaseResponse purchase(UUID userId, UUID sessionItemId, String idempotencyKey) {

    // Layer 1 idempotency — Redis fast path
    var cached = idempotencyService.lookup(idempotencyKey);
    if (cached.isPresent()) {
      log.debug("Idempotency cache hit for key={}", idempotencyKey);
      return cached.get();
    }

    return retryTemplate.execute(ctx -> doPurchase(userId, sessionItemId, idempotencyKey));
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
  public PurchaseResponse doPurchase(UUID userId, UUID sessionItemId, String idempotencyKey) {

    // Load item with shared lock
    var item =
        itemRepository
            .findByIdForPurchase(sessionItemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_ITEM_NOT_FOUND));

    // Load user balance for rule evaluation
    var balance =
        userRepository
            .findBalanceById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // Run all eligibility rules in order
    var context = new PurchaseContext(userId, item, balance, clock);
    rules.forEach(rule -> rule.check(context));

    // Atomic stock decrement — uses optimistic version check
    var stockUpdated = itemRepository.decrementSold(item.getId(), item.getVersion());
    if (stockUpdated == 0) {
      // Version mismatch or sold out — trigger retry
      throw new OptimisticLockingFailureException("Stock contention on item " + item.getId());
    }

    // Atomic balance deduction — WHERE balance >= price
    var balanceUpdated = userRepository.deductBalance(userId, item.getSalePrice());
    if (balanceUpdated == 0) {
      throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
    }

    // Persist order (via OrderService — same transaction)
    var newBalance = userRepository.findBalanceById(userId).orElse(BigDecimal.ZERO);

    var order =
        orderService.createFlashSaleOrder(
            userId,
            idempotencyKey,
            item.getProduct().getId(),
            item.getProduct().getName(),
            item.getId(),
            item.getSalePrice(),
            newBalance);

    // Publish outbox event for inventory sync
    outboxPublisher.publish(
        "FLASH_SALE_PURCHASED",
        "ORDER",
        order.getId(),
        Map.of(
            "productId", item.getProduct().getId().toString(),
            "quantity", 1,
            "orderId", order.getId().toString()));

    var response =
        new PurchaseResponse(order.getId(), item.getSalePrice(), newBalance, order.getCreatedAt());

    // Cache after commit — Layer 2 idempotency fast path
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            idempotencyService.cache(idempotencyKey, response, Duration.ofHours(24));
          }
        });

    return response;
  }
}
