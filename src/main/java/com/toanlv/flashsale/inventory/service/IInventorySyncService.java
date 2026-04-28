package com.toanlv.flashsale.inventory.service;

import java.util.UUID;

import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;
import com.toanlv.flashsale.inventory.domain.Inventory;

public interface IInventorySyncService {
  void handlePurchase(OutboxEvent event);

  void handleRestock(OutboxEvent event);

  void handleRefund(OutboxEvent event);

  Inventory adminRestock(UUID productId, int quantity);

  Inventory getInventory(UUID productId);
}
