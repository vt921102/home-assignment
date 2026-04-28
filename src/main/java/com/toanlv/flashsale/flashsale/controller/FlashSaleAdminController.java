package com.toanlv.flashsale.flashsale.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.toanlv.flashsale.flashsale.dto.AddSessionItemRequest;
import com.toanlv.flashsale.flashsale.dto.CreateSessionRequest;
import com.toanlv.flashsale.flashsale.dto.FlashSaleItemDto;
import com.toanlv.flashsale.flashsale.dto.SessionDto;
import com.toanlv.flashsale.flashsale.service.IFlashSaleQueryService;
import com.toanlv.flashsale.flashsale.service.ISessionAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin — Flash Sale", description = "Flash sale session and item management")
@RestController
@RequestMapping("/api/v1/admin/flash-sale")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FlashSaleAdminController {

  private final ISessionAdminService sessionAdminService;
  private final IFlashSaleQueryService queryService;

  @Operation(summary = "List sessions for a date")
  @GetMapping("/sessions")
  public ResponseEntity<List<SessionDto>> getSessions(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(queryService.getSessionsByDate(date));
  }

  @Operation(summary = "Create flash sale session")
  @PostMapping("/sessions")
  public ResponseEntity<SessionDto> createSession(
      @Valid @RequestBody CreateSessionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(sessionAdminService.createSession(request));
  }

  @Operation(summary = "Add item to session")
  @PostMapping("/sessions/{sessionId}/items")
  public ResponseEntity<FlashSaleItemDto> addItem(
      @PathVariable UUID sessionId, @Valid @RequestBody AddSessionItemRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(sessionAdminService.addItem(sessionId, request));
  }

  @Operation(summary = "Get items in a session")
  @GetMapping("/sessions/{sessionId}/items")
  public ResponseEntity<List<FlashSaleItemDto>> getItems(@PathVariable UUID sessionId) {
    return ResponseEntity.ok(sessionAdminService.getItemsBySession(sessionId));
  }

  @Operation(summary = "Deactivate a session")
  @DeleteMapping("/sessions/{sessionId}")
  public ResponseEntity<SessionDto> deactivateSession(@PathVariable UUID sessionId) {
    return ResponseEntity.ok(sessionAdminService.deactivateSession(sessionId));
  }
}
