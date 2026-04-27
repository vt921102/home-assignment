package com.toanlv.flashsale.inventory;

import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;
import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.common.outbox.service.OutboxPublisher;
import com.toanlv.flashsale.inventory.domain.InventoryAuditLog;
import com.toanlv.flashsale.inventory.repository.InventoryAuditLogRepository;
import com.toanlv.flashsale.inventory.repository.InventoryRepository;
import com.toanlv.flashsale.inventory.service.InventorySyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventorySyncServiceTest {

    @Mock InventoryRepository        inventoryRepository;
    @Mock InventoryAuditLogRepository auditLogRepository;
    @Mock OutboxPublisher             outboxPublisher;

    private InventorySyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new InventorySyncService(
                inventoryRepository, auditLogRepository, outboxPublisher);
    }

    // ----------------------------------------------------------------
    // handlePurchase
    // ----------------------------------------------------------------

    @Test
    void handlePurchase_decrementsInventory_onFirstProcessing() {
        var productId = UUID.randomUUID();
        var event     = buildEvent("FLASH_SALE_PURCHASED", productId, 1);

        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.decrementAvailable(productId, 1)).thenReturn(1);

        syncService.handlePurchase(event);

        var auditCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());

        var audit = auditCaptor.getValue();
        assertThat(audit.getProductId()).isEqualTo(productId);
        assertThat(audit.getSourceEventId()).isEqualTo(event.getId());
        assertThat(audit.getDelta()).isEqualTo(-1);
        assertThat(audit.getReason()).isEqualTo("FLASH_SALE_PURCHASE");

        verify(inventoryRepository).decrementAvailable(productId, 1);
    }

    @Test
    void handlePurchase_skipsIdempotently_whenAuditLogAlreadyExists() {
        var productId = UUID.randomUUID();
        var event     = buildEvent("FLASH_SALE_PURCHASED", productId, 1);

        when(auditLogRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        syncService.handlePurchase(event);

        verify(inventoryRepository, never())
                .decrementAvailable(any(), anyInt());
    }

    @Test
    void handlePurchase_throwsInconsistency_whenInventoryUpdateFails() {
        var productId = UUID.randomUUID();
        var event     = buildEvent("FLASH_SALE_PURCHASED", productId, 1);

        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.decrementAvailable(productId, 1)).thenReturn(0);

        assertThatThrownBy(() -> syncService.handlePurchase(event))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVENTORY_INSUFFICIENT));
    }

    // ----------------------------------------------------------------
    // handleRestock
    // ----------------------------------------------------------------

    @Test
    void handleRestock_incrementsInventory_onFirstProcessing() {
        var productId = UUID.randomUUID();
        var event     = buildEvent("PRODUCT_RESTOCKED", productId, 50);

        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.incrementAvailable(productId, 50)).thenReturn(1);

        syncService.handleRestock(event);

        var auditCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());

        var audit = auditCaptor.getValue();
        assertThat(audit.getDelta()).isEqualTo(50);
        assertThat(audit.getReason()).isEqualTo("PRODUCT_RESTOCKED");

        verify(inventoryRepository).incrementAvailable(productId, 50);
    }

    @Test
    void handleRestock_skipsIdempotently_whenAuditLogAlreadyExists() {
        var productId = UUID.randomUUID();
        var event     = buildEvent("PRODUCT_RESTOCKED", productId, 50);

        when(auditLogRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        syncService.handleRestock(event);

        verify(inventoryRepository, never())
                .incrementAvailable(any(), anyInt());
    }

    @Test
    void handleRestock_throwsNotFound_whenInventoryUpdateFails() {
        var productId = UUID.randomUUID();
        var event     = buildEvent("PRODUCT_RESTOCKED", productId, 50);

        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.incrementAvailable(productId, 50)).thenReturn(0);

        assertThatThrownBy(() -> syncService.handleRestock(event))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVENTORY_NOT_FOUND));
    }

    // ----------------------------------------------------------------
    // handleRefund
    // ----------------------------------------------------------------

    @Test
    void handleRefund_releasesReservedStock() {
        var productId = UUID.randomUUID();
        var event     = buildEvent("ORDER_REFUNDED", productId, 1);

        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.releaseReserved(productId, 1)).thenReturn(1);

        syncService.handleRefund(event);

        var auditCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());

        assertThat(auditCaptor.getValue().getDelta()).isEqualTo(1);
        assertThat(auditCaptor.getValue().getReason()).isEqualTo("ORDER_REFUND");
        verify(inventoryRepository).releaseReserved(productId, 1);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private OutboxEvent buildEvent(
            String eventType,
            UUID productId,
            int quantity) {
        return OutboxEvent.create(
                eventType,
                "ORDER",
                UUID.randomUUID(),
                Map.of(
                        "productId", productId.toString(),
                        "quantity",  quantity,
                        "orderId",   UUID.randomUUID().toString()
                )
        );
    }
}
