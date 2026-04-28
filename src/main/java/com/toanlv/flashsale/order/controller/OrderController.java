package com.toanlv.flashsale.order.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toanlv.flashsale.common.security.AuthenticatedUser;
import com.toanlv.flashsale.order.dto.BalanceTransactionDto;
import com.toanlv.flashsale.order.dto.OrderDto;
import com.toanlv.flashsale.order.dto.OrderSummaryDto;
import com.toanlv.flashsale.order.service.IOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Orders", description = "Order history and balance transactions")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final IOrderService orderService;

  @Operation(
      summary = "Get my orders",
      description = "Paginated order history for the authenticated user.")
  @GetMapping("/my")
  public ResponseEntity<Page<OrderSummaryDto>> getMyOrders(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
    return ResponseEntity.ok(orderService.findByUser(user.userId(), pageable));
  }

  @Operation(
      summary = "Get order detail",
      description =
          "Full order detail including line items. " + "Only accessible by the order owner.")
  @GetMapping("/my/{orderId}")
  public ResponseEntity<OrderDto> getMyOrder(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID orderId) {
    return ResponseEntity.ok(orderService.findByIdAndUser(orderId, user.userId()));
  }

  @Operation(
      summary = "Get my balance transactions",
      description = "Paginated balance transaction history " + "for the authenticated user.")
  @GetMapping("/my/transactions")
  public ResponseEntity<Page<BalanceTransactionDto>> getMyTransactions(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
    return ResponseEntity.ok(orderService.findTransactionsByUser(user.userId(), pageable));
  }
}
