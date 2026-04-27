package com.toanlv.flashsale.flashsale.dto;


import com.toanlv.flashsale.flashsale.domain.FlashSaleSession;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SessionDto(
        UUID            id,
        String          name,
        LocalDate       saleDate,
        LocalTime       startTime,
        LocalTime       endTime,
        boolean         active,
        List<FlashSaleItemDto> items,
        Instant         createdAt
) {
    public static SessionDto from(FlashSaleSession session) {
        return new SessionDto(
                session.getId(),
                session.getName(),
                session.getSaleDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.isActive(),
                session.getItems().stream()
                        .map(FlashSaleItemDto::from)
                        .toList(),
                session.getCreatedAt()
        );
    }
}
