package com.toanlv.flashsale.inventory.outbox;

import org.springframework.stereotype.Component;

import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;
import com.toanlv.flashsale.common.outbox.handler.OutboxEventHandler;
import com.toanlv.flashsale.inventory.service.IInventorySyncService;

import lombok.RequiredArgsConstructor;

/**
 * Handles FLASH_SALE_PURCHASED outbox events. Decrements available inventory when a flash sale
 * purchase is confirmed.
 */
@Component
@RequiredArgsConstructor
public class PurchaseSyncHandler implements OutboxEventHandler {

  private final IInventorySyncService syncService;

  @Override
  public String supportedType() {
    return "FLASH_SALE_PURCHASED";
  }

  @Override
  public void handle(OutboxEvent event) throws Exception {
    syncService.handlePurchase(event);
  }
}
