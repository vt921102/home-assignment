package com.toanlv.flashsale.inventory.outbox;


import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;
import com.toanlv.flashsale.common.outbox.handler.OutboxEventHandler;
import com.toanlv.flashsale.inventory.service.InventorySyncService;
import org.springframework.stereotype.Component;

/**
 * Handles PRODUCT_RESTOCKED outbox events.
 * Increments available inventory when admin restocks a product.
 */
@Component
public class RestockSyncHandler implements OutboxEventHandler {

    private final InventorySyncService syncService;

    public RestockSyncHandler(InventorySyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public String supportedType() {
        return "PRODUCT_RESTOCKED";
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        syncService.handleRestock(event);
    }
}
