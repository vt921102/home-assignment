package com.toanlv.flashsale.inventory.service;

import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;
import com.toanlv.flashsale.common.outbox.service.OutboxPublisher;
import com.toanlv.flashsale.inventory.domain.Inventory;
import com.toanlv.flashsale.inventory.domain.InventoryAuditLog;
import com.toanlv.flashsale.inventory.repository.InventoryAuditLogRepository;
import com.toanlv.flashsale.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class InventorySyncService {

    private static final Logger log =
            LoggerFactory.getLogger(InventorySyncService.class);

    private final InventoryRepository         inventoryRepository;
    private final InventoryAuditLogRepository auditLogRepository;
    private final OutboxPublisher             outboxPublisher;

    public InventorySyncService(
            InventoryRepository inventoryRepository,
            InventoryAuditLogRepository auditLogRepository,
            OutboxPublisher outboxPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.auditLogRepository  = auditLogRepository;
        this.outboxPublisher     = outboxPublisher;
    }

    // ----------------------------------------------------------------
    // Flash Sale Purchase — decrement available stock
    // ----------------------------------------------------------------

    /**
     * Handles FLASH_SALE_PURCHASED outbox event.
     *
     * Idempotency is enforced at two levels:
     *   1. audit log UNIQUE on source_event_id — duplicate event
     *      throws DataIntegrityViolationException, caught and skipped.
     *   2. UPDATE WHERE available >= qty — never goes negative.
     */
    @Transactional
    public void handlePurchase(OutboxEvent event) {
        var productId = extractUUID(event, "productId");
        var quantity  = extractInt(event, "quantity");

        if (!tryInsertAuditLog(productId, event.getId(),
                -quantity, "FLASH_SALE_PURCHASE")) {
            log.info("Duplicate purchase event skipped: eventId={}",
                    event.getId());
            return;
        }

        var updated = inventoryRepository
                .decrementAvailable(productId, quantity);

        if (updated == 0) {
            throw new BusinessException(
                    ErrorCode.INVENTORY_INSUFFICIENT,
                    "Cannot decrement inventory for productId=" + productId
                            + ", quantity=" + quantity);
        }

        log.debug("Inventory decremented: productId={}, qty={}",
                productId, quantity);
    }

    // ----------------------------------------------------------------
    // Product Restocked — increment available stock
    // ----------------------------------------------------------------

    /**
     * Handles PRODUCT_RESTOCKED outbox event.
     */
    @Transactional
    public void handleRestock(OutboxEvent event) {
        var productId = extractUUID(event, "productId");
        var quantity  = extractInt(event, "quantity");

        if (!tryInsertAuditLog(productId, event.getId(),
                quantity, "PRODUCT_RESTOCKED")) {
            log.info("Duplicate restock event skipped: eventId={}",
                    event.getId());
            return;
        }

        var updated = inventoryRepository
                .incrementAvailable(productId, quantity);

        if (updated == 0) {
            throw new BusinessException(
                    ErrorCode.INVENTORY_NOT_FOUND,
                    "Inventory not found for productId=" + productId);
        }

        log.debug("Inventory restocked: productId={}, qty={}",
                productId, quantity);
    }

    // ----------------------------------------------------------------
    // Order Refund — release reserved stock back to available
    // ----------------------------------------------------------------

    /**
     * Handles ORDER_REFUNDED outbox event.
     */
    @Transactional
    public void handleRefund(OutboxEvent event) {
        var productId = extractUUID(event, "productId");
        var quantity  = extractInt(event, "quantity");

        if (!tryInsertAuditLog(productId, event.getId(),
                quantity, "ORDER_REFUND")) {
            log.info("Duplicate refund event skipped: eventId={}",
                    event.getId());
            return;
        }

        var updated = inventoryRepository
                .releaseReserved(productId, quantity);

        if (updated == 0) {
            throw new BusinessException(
                    ErrorCode.INVENTORY_INSUFFICIENT,
                    "Cannot release reserved stock for productId=" + productId);
        }

        log.debug("Inventory released: productId={}, qty={}",
                productId, quantity);
    }

    // ----------------------------------------------------------------
    // Direct restock — called from admin controller
    // ----------------------------------------------------------------

    /**
     * Admin-triggered direct restock.
     * Publishes an outbox event so inventory sync is consistent
     * with purchase-triggered sync.
     */
    @Transactional
    public Inventory adminRestock(UUID productId, int quantity) {
        var inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVENTORY_NOT_FOUND));

        inventory.restock(quantity);
        var saved = inventoryRepository.save(inventory);

        outboxPublisher.publish(
                "PRODUCT_RESTOCKED",
                "INVENTORY",
                productId,
                Map.of(
                        "productId", productId.toString(),
                        "quantity",  quantity
                )
        );

        return saved;
    }

    // ----------------------------------------------------------------
    // Query — called from admin controller
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public Inventory getInventory(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVENTORY_NOT_FOUND));
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    /**
     * Insert audit log with idempotency guard.
     *
     * @return true if inserted (first time), false if duplicate
     */
    private boolean tryInsertAuditLog(
            UUID productId,
            UUID sourceEventId,
            int delta,
            String reason) {
        try {
            auditLogRepository.save(InventoryAuditLog.create(
                    productId, sourceEventId, delta, reason));
            return true;
        } catch (DataIntegrityViolationException ex) {
            // UNIQUE constraint on source_event_id violated — duplicate event
            return false;
        }
    }

    private UUID extractUUID(OutboxEvent event, String key) {
        var value = event.getPayload().get(key);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing payload key '" + key
                            + "' in event " + event.getId());
        }
        return UUID.fromString(value.toString());
    }

    private int extractInt(OutboxEvent event, String key) {
        var value = event.getPayload().get(key);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing payload key '" + key
                            + "' in event " + event.getId());
        }
        return ((Number) value).intValue();
    }
}
