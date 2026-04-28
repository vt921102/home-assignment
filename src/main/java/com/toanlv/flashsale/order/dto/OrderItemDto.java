package com.toanlv.flashsale.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.toanlv.flashsale.order.domain.OrderItem;

public record OrderItemDto(
    UUID id,
    UUID productId,
    String productName,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal,
    UUID sourceRefId) {
  public static OrderItemDto from(OrderItem item) {
    return new OrderItemDto(
        item.getId(),
        item.getProductId(),
        item.getProductName(),
        item.getQuantity(),
        item.getUnitPrice(),
        item.getSubtotal(),
        item.getSourceRefId());
  }
}
