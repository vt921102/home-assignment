package com.toanlv.flashsale.flashsale.service;

import java.util.UUID;

import com.toanlv.flashsale.flashsale.dto.PurchaseResponse;

public interface IPurchaseService {
  PurchaseResponse purchase(UUID userId, UUID sessionItemId, String idempotencyKey);

  PurchaseResponse doPurchase(UUID userId, UUID sessionItemId, String idempotencyKey);
}
