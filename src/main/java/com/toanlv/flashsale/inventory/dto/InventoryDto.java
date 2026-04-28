package com.toanlv.flashsale.inventory.dto;

import java.time.Instant;
import java.util.UUID;

import com.toanlv.flashsale.inventory.domain.Inventory;

public record InventoryDto(
    UUID id,
    UUID productId,
    int totalQuantity,
    int reservedQuantity,
    int availableQuantity,
    Instant updatedAt) {
  public static InventoryDto from(Inventory inventory) {
    return new InventoryDto(
        inventory.getId(),
        inventory.getProductId(),
        inventory.getTotalQuantity(),
        inventory.getReservedQuantity(),
        inventory.getAvailableQuantity(),
        inventory.getUpdatedAt());
  }
}
