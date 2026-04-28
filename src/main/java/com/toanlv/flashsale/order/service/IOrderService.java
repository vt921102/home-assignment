package com.toanlv.flashsale.order.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.toanlv.flashsale.order.domain.Order;
import com.toanlv.flashsale.order.dto.BalanceTransactionDto;
import com.toanlv.flashsale.order.dto.OrderDto;
import com.toanlv.flashsale.order.dto.OrderSummaryDto;

public interface IOrderService {
  Page<OrderSummaryDto> findByUser(UUID userId, Pageable pageable);

  OrderDto findByIdAndUser(UUID orderId, UUID userId);

  Page<BalanceTransactionDto> findTransactionsByUser(UUID userId, Pageable pageable);

  Order createFlashSaleOrder(
      UUID userId,
      String idempotencyKey,
      UUID productId,
      String productName,
      UUID sessionItemId,
      BigDecimal unitPrice,
      BigDecimal newBalance);

  Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
