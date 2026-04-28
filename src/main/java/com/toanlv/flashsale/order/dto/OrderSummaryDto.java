package com.toanlv.flashsale.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.toanlv.flashsale.order.domain.Order;
import com.toanlv.flashsale.order.domain.OrderStatus;

public record OrderSummaryDto(
    UUID id,
    OrderStatus status,
    BigDecimal totalAmount,
    String orderType,
    int itemCount,
    Instant createdAt) {
  public static OrderSummaryDto from(Order order) {
    return new OrderSummaryDto(
        order.getId(),
        order.getStatus(),
        order.getTotalAmount(),
        order.getOrderType(),
        order.getItems().size(),
        order.getCreatedAt());
  }
}
