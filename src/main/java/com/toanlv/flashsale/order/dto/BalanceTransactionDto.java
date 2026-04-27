package com.toanlv.flashsale.order.dto;


import com.toanlv.flashsale.order.domain.BalanceTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BalanceTransactionDto(
        UUID       id,
        UUID       orderId,
        BigDecimal amount,
        String     direction,
        BigDecimal balanceAfter,
        String     reason,
        Instant    createdAt
) {
    public static BalanceTransactionDto from(BalanceTransaction tx) {
        return new BalanceTransactionDto(
                tx.getId(),
                tx.getOrderId(),
                tx.getAmount(),
                tx.getDirection().name(),
                tx.getBalanceAfter(),
                tx.getReason(),
                tx.getCreatedAt()
        );
    }
}
