package com.toanlv.flashsale.order.service;


import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.order.domain.BalanceTransaction;
import com.toanlv.flashsale.order.domain.Order;
import com.toanlv.flashsale.order.domain.OrderItem;
import com.toanlv.flashsale.order.dto.BalanceTransactionDto;
import com.toanlv.flashsale.order.dto.OrderDto;
import com.toanlv.flashsale.order.dto.OrderSummaryDto;
import com.toanlv.flashsale.order.repository.BalanceTransactionRepository;
import com.toanlv.flashsale.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository              orderRepository;
    private final BalanceTransactionRepository balanceTxRepository;

    public OrderService(
            OrderRepository orderRepository,
            BalanceTransactionRepository balanceTxRepository) {
        this.orderRepository      = orderRepository;
        this.balanceTxRepository  = balanceTxRepository;
    }

    // ----------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------

    /**
     * Paginated order history for the authenticated user.
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> findByUser(
            UUID userId,
            Pageable pageable) {
        return orderRepository
                .findByUserId(userId, pageable)
                .map(OrderSummaryDto::from);
    }

    /**
     * Find a single order — only accessible by the owning user.
     */
    @Transactional(readOnly = true)
    public OrderDto findByIdAndUser(UUID orderId, UUID userId) {
        var order = orderRepository
                .findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        return OrderDto.from(order);
    }

    /**
     * Balance transaction history for the authenticated user.
     */
    @Transactional(readOnly = true)
    public Page<BalanceTransactionDto> findTransactionsByUser(
            UUID userId,
            Pageable pageable) {
        return balanceTxRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(BalanceTransactionDto::from);
    }

    // ----------------------------------------------------------------
    // Write — called from PurchaseService
    // ----------------------------------------------------------------

    /**
     * Persist a completed flash sale order together with its
     * balance transaction record.
     *
     * Called within PurchaseService's @Transactional boundary —
     * both writes are atomic with the stock decrement and
     * balance deduction.
     *
     * @param userId         purchasing user
     * @param idempotencyKey client-provided unique key
     * @param productId      product being purchased
     * @param productName    snapshot of product name at time of purchase
     * @param sessionItemId  flash_sale_session_item reference
     * @param unitPrice      sale price (not base price)
     * @param newBalance     user balance after deduction
     * @return persisted Order
     */
    @Transactional
    public Order createFlashSaleOrder(
            UUID userId,
            String idempotencyKey,
            UUID productId,
            String productName,
            UUID sessionItemId,
            BigDecimal unitPrice,
            BigDecimal newBalance) {

        var order = Order.createFlashSaleOrder(
                userId, idempotencyKey, unitPrice);

        var item = OrderItem.of(
                productId,
                productName,
                1,
                unitPrice,
                sessionItemId);

        order.addItem(item);
        order.complete();

        var saved = orderRepository.save(order);

        balanceTxRepository.save(
                BalanceTransaction.debit(
                        userId,
                        saved.getId(),
                        unitPrice,
                        newBalance,
                        "FLASH_SALE_PURCHASE"));

        return saved;
    }

    /**
     * Find order by idempotency key.
     * Returns empty if not found — caller decides what to do.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Order> findByIdempotencyKey(
            String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(idempotencyKey);
    }
}
