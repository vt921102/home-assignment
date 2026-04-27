package com.toanlv.flashsale.flashsale;

import com.toanlv.flashsale.auth.repository.UserRepository;
import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.common.outbox.service.OutboxPublisher;
import com.toanlv.flashsale.flashsale.domain.FlashSaleSession;
import com.toanlv.flashsale.flashsale.domain.FlashSaleSessionItem;
import com.toanlv.flashsale.flashsale.dto.PurchaseResponse;
import com.toanlv.flashsale.flashsale.repository.FlashSaleSessionItemRepository;
import com.toanlv.flashsale.flashsale.service.IdempotencyService;
import com.toanlv.flashsale.flashsale.service.PurchaseService;
import com.toanlv.flashsale.flashsale.strategy.BalanceRule;
import com.toanlv.flashsale.flashsale.strategy.DailyLimitRule;
import com.toanlv.flashsale.flashsale.strategy.StockRule;
import com.toanlv.flashsale.flashsale.strategy.TimeWindowRule;
import com.toanlv.flashsale.order.domain.Order;
import com.toanlv.flashsale.order.service.OrderService;
import com.toanlv.flashsale.product.domain.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock FlashSaleSessionItemRepository itemRepository;
    @Mock UserRepository                 userRepository;
    @Mock OrderService                   orderService;
    @Mock OutboxPublisher                outboxPublisher;
    @Mock IdempotencyService             idempotencyService;
    @Mock TimeWindowRule                 timeWindowRule;
    @Mock StockRule                      stockRule;
    @Mock BalanceRule                    balanceRule;
    @Mock DailyLimitRule                 dailyLimitRule;

    // Fixed clock: within a valid flash sale window
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-01-01T08:30:00Z"), ZoneOffset.UTC);

    private PurchaseService purchaseService;

    private UUID userId;
    private UUID sessionItemId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(
                itemRepository, userRepository, orderService,
                outboxPublisher, idempotencyService,
                timeWindowRule, stockRule, balanceRule, dailyLimitRule,
                clock);

        userId          = UUID.randomUUID();
        sessionItemId   = UUID.randomUUID();
        idempotencyKey  = UUID.randomUUID().toString();

        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // ----------------------------------------------------------------
    // Idempotency cache hit
    // ----------------------------------------------------------------

    @Test
    void purchase_returnsCachedResponse_onIdempotencyCacheHit() {
        var cached = new PurchaseResponse(
                UUID.randomUUID(), BigDecimal.valueOf(99_000),
                BigDecimal.valueOf(901_000), Instant.now());

        when(idempotencyService.lookup(idempotencyKey))
                .thenReturn(Optional.of(cached));

        var result = purchaseService.purchase(
                userId, sessionItemId, idempotencyKey);

        assertThat(result).isEqualTo(cached);
        verify(itemRepository, never()).findByIdForPurchase(any());
    }

    // ----------------------------------------------------------------
    // Successful purchase
    // ----------------------------------------------------------------

    @Test
    void doPurchase_succeeds_whenAllConditionsMet() {
        var item  = buildItem(BigDecimal.valueOf(99_000));
        var order = mockOrder(item.getSalePrice());

        when(itemRepository.findByIdForPurchase(sessionItemId))
                .thenReturn(Optional.of(item));
        when(userRepository.findBalanceById(userId))
                .thenReturn(Optional.of(BigDecimal.valueOf(1_000_000)))
                .thenReturn(Optional.of(BigDecimal.valueOf(901_000)));
        when(itemRepository.decrementSold(any(), anyLong())).thenReturn(1);
        when(userRepository.deductBalance(any(), any())).thenReturn(1);
        when(orderService.createFlashSaleOrder(
                any(), anyString(), any(), anyString(), any(), any(), any()))
                .thenReturn(order);

        var result = purchaseService.doPurchase(
                userId, sessionItemId, idempotencyKey);

        var orderId = order.getId();
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.amountPaid()).isEqualTo(BigDecimal.valueOf(99_000));

        verify(outboxPublisher).publish(
                eq("FLASH_SALE_PURCHASED"), eq("ORDER"),
                eq(orderId), any());
    }

    // ----------------------------------------------------------------
    // Stock contention
    // ----------------------------------------------------------------

    @Test
    void doPurchase_throwsOptimisticLock_whenStockDecrementFails() {
        var item = buildItem(BigDecimal.valueOf(99_000));

        when(itemRepository.findByIdForPurchase(sessionItemId))
                .thenReturn(Optional.of(item));
        when(userRepository.findBalanceById(userId))
                .thenReturn(Optional.of(BigDecimal.valueOf(1_000_000)));
        when(itemRepository.decrementSold(any(), anyLong())).thenReturn(0);

        assertThatThrownBy(() ->
                purchaseService.doPurchase(
                        userId, sessionItemId, idempotencyKey))
                .isInstanceOf(OptimisticLockingFailureException.class);

        verify(userRepository, never()).deductBalance(any(), any());
        verify(orderService, never())
                .createFlashSaleOrder(any(), any(), any(), any(), any(), any(), any());
    }

    // ----------------------------------------------------------------
    // Insufficient balance (DB level guard)
    // ----------------------------------------------------------------

    @Test
    void doPurchase_throwsInsufficientBalance_whenDeductFails() {
        var item = buildItem(BigDecimal.valueOf(99_000));

        when(itemRepository.findByIdForPurchase(sessionItemId))
                .thenReturn(Optional.of(item));
        when(userRepository.findBalanceById(userId))
                .thenReturn(Optional.of(BigDecimal.valueOf(1_000_000)));
        when(itemRepository.decrementSold(any(), anyLong())).thenReturn(1);
        when(userRepository.deductBalance(any(), any())).thenReturn(0);

        assertThatThrownBy(() ->
                purchaseService.doPurchase(
                        userId, sessionItemId, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));

        verify(orderService, never())
                .createFlashSaleOrder(any(), any(), any(), any(), any(), any(), any());
    }

    // ----------------------------------------------------------------
    // Item not found
    // ----------------------------------------------------------------

    @Test
    void doPurchase_throwsNotFound_whenSessionItemMissing() {
        when(itemRepository.findByIdForPurchase(sessionItemId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                purchaseService.doPurchase(
                        userId, sessionItemId, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SESSION_ITEM_NOT_FOUND));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private FlashSaleSessionItem buildItem(BigDecimal salePrice) {
        var product = mock(Product.class);
        lenient().when(product.getId()).thenReturn(UUID.randomUUID());
        lenient().when(product.getName()).thenReturn("iPhone 15");

        var session = FlashSaleSession.create(
                "Morning Sale",
                LocalDate.of(2026, 1, 1),
                LocalTime.of(8, 0),
                LocalTime.of(10, 0));

        return FlashSaleSessionItem.create(
                session, product, salePrice, 10, 1);
    }

    private Order mockOrder(BigDecimal amount) {
        var order = mock(Order.class);
        when(order.getId()).thenReturn(UUID.randomUUID());
        when(order.getCreatedAt()).thenReturn(Instant.now());
        return order;
    }
}
