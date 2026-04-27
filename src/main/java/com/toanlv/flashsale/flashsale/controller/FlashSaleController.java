package com.toanlv.flashsale.flashsale.controller;


import com.toanlv.flashsale.common.security.AuthenticatedUser;
import com.toanlv.flashsale.flashsale.dto.FlashSaleItemDto;
import com.toanlv.flashsale.flashsale.dto.PurchaseRequest;
import com.toanlv.flashsale.flashsale.dto.PurchaseResponse;
import com.toanlv.flashsale.flashsale.service.FlashSaleQueryService;
import com.toanlv.flashsale.flashsale.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Flash Sale",
        description = "Browse and purchase flash sale items")
@RestController
@RequestMapping("/api/v1/flash-sale")
public class FlashSaleController {

    private final FlashSaleQueryService queryService;
    private final PurchaseService       purchaseService;

    public FlashSaleController(
            FlashSaleQueryService queryService,
            PurchaseService purchaseService) {
        this.queryService    = queryService;
        this.purchaseService = purchaseService;
    }

    @Operation(
            summary = "Get current flash sale items",
            description = "Returns all products available in active "
                    + "flash sale sessions at the current time. "
                    + "Response is cached for 2 seconds.")
    @GetMapping("/current")
    public ResponseEntity<List<FlashSaleItemDto>> getCurrent() {
        return ResponseEntity.ok(queryService.getCurrentItems());
    }

    @Operation(
            summary = "Purchase a flash sale item",
            description = "Purchases a flash sale item for the authenticated user. "
                    + "Requires X-Idempotency-Key header (UUID v4) to prevent "
                    + "duplicate orders on client retry. "
                    + "Limited to 1 purchase per user per day.")
    @PostMapping("/purchase")
    public ResponseEntity<PurchaseResponse> purchase(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PurchaseRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
        var response = purchaseService.purchase(
                user.userId(),
                request.sessionItemId(),
                idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
