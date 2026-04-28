package com.toanlv.flashsale.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.toanlv.flashsale.order.domain.Order;
import com.toanlv.flashsale.order.domain.OrderStatus;

public record OrderDto(
    UUID id,
    UUID userId,
    OrderStatus status,
    BigDecimal totalAmount,
    String orderType,
    List<OrderItemDto> items,
    Instant createdAt,
    Instant updatedAt) {
  public static OrderDto from(Order order) {
    return new OrderDto(
        order.getId(),
        order.getUserId(),
        order.getStatus(),
        order.getTotalAmount(),
        order.getOrderType(),
        order.getItems().stream().map(OrderItemDto::from).toList(),
        order.getCreatedAt(),
        order.getUpdatedAt());
  }
}
