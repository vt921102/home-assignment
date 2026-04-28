package com.toanlv.flashsale.flashsale.service;

import java.time.Duration;
import java.util.Optional;

import com.toanlv.flashsale.flashsale.dto.PurchaseResponse;

public interface IIdempotencyService {
  Optional<PurchaseResponse> lookup(String key);

  void cache(String key, PurchaseResponse response, Duration ttl);
}
