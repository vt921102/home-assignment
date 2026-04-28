package com.toanlv.flashsale.inventory.outbox;

import org.springframework.stereotype.Component;

import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;
import com.toanlv.flashsale.common.outbox.handler.OutboxEventHandler;
import com.toanlv.flashsale.inventory.service.IInventorySyncService;

import lombok.RequiredArgsConstructor;

/**
 * Handles PRODUCT_RESTOCKED outbox events. Increments available inventory when admin restocks a
 * product.
 */
@Component
@RequiredArgsConstructor
public class RestockSyncHandler implements OutboxEventHandler {

  private final IInventorySyncService syncService;

  @Override
  public String supportedType() {
    return "PRODUCT_RESTOCKED";
  }

  @Override
  public void handle(OutboxEvent event) throws Exception {
    syncService.handleRestock(event);
  }
}
