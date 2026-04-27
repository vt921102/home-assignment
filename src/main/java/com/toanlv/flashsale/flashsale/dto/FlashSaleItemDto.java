package com.toanlv.flashsale.flashsale.dto;


import com.toanlv.flashsale.flashsale.domain.FlashSaleSessionItem;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record FlashSaleItemDto(
        UUID       sessionItemId,
        UUID       productId,
        String     productName,
        String     imageUrl,
        BigDecimal salePrice,
        BigDecimal basePrice,
        int        remainingQuantity,
        LocalTime  sessionStartTime,
        LocalTime  sessionEndTime
) {
    public static FlashSaleItemDto from(FlashSaleSessionItem item) {
        return new FlashSaleItemDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getImageUrl(),
                item.getSalePrice(),
                item.getProduct().getBasePrice(),
                item.getRemainingQuantity(),
                item.getSession().getStartTime(),
                item.getSession().getEndTime()
        );
    }
}
