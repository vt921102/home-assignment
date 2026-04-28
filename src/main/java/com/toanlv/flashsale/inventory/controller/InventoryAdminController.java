package com.toanlv.flashsale.inventory.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toanlv.flashsale.inventory.dto.InventoryDto;
import com.toanlv.flashsale.inventory.dto.RestockRequest;
import com.toanlv.flashsale.inventory.service.IInventorySyncService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin — Inventory", description = "Inventory management and restocking")
@RestController
@RequestMapping("/api/v1/admin/inventory")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class InventoryAdminController {

  private final IInventorySyncService inventorySyncService;

  @Operation(summary = "Get inventory for a product")
  @GetMapping("/{productId}")
  public ResponseEntity<InventoryDto> getInventory(@PathVariable UUID productId) {
    return ResponseEntity.ok(InventoryDto.from(inventorySyncService.getInventory(productId)));
  }

  @Operation(
      summary = "Restock a product",
      description =
          "Adds stock to a product's available quantity. "
              + "Publishes PRODUCT_RESTOCKED event for audit trail.")
  @PostMapping("/{productId}/restock")
  public ResponseEntity<InventoryDto> restock(
      @PathVariable UUID productId, @Valid @RequestBody RestockRequest request) {
    return ResponseEntity.ok(
        InventoryDto.from(inventorySyncService.adminRestock(productId, request.quantity())));
  }
}
