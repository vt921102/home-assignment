package com.toanlv.flashsale.flashsale.service.impl;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toanlv.flashsale.flashsale.dto.PurchaseResponse;
import com.toanlv.flashsale.flashsale.service.IIdempotencyService;

import lombok.RequiredArgsConstructor;

/**
 * Redis-backed idempotency cache for purchase requests.
 *
 * <p>Stores serialised PurchaseResponse keyed by idempotency_key. On cache hit, the original
 * response is returned without re-executing the purchase logic — guarantees exactly-once semantics
 * for retries.
 *
 * <p>Two-layer idempotency strategy: Layer 1 (this class) — Redis cache for fast path (~1ms) Layer
 * 2 — DB UNIQUE constraint on orders.idempotency_key (correctness guard)
 *
 * <p>If Redis is unavailable, Layer 2 catches duplicate inserts via DataIntegrityViolationException
 * in PurchaseService.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService implements IIdempotencyService {

  private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

  private static final String KEY_PREFIX = "idempotency:purchase:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Look up a cached response for the given idempotency key.
   *
   * @param key idempotency key from the client request
   * @return cached PurchaseResponse if found, empty otherwise
   */
  @Override
  public Optional<PurchaseResponse> lookup(String key) {
    try {
      var cached = redisTemplate.opsForValue().get(KEY_PREFIX + key);
      if (cached == null) return Optional.empty();
      return Optional.of(objectMapper.readValue(cached, PurchaseResponse.class));
    } catch (Exception ex) {
      log.warn("Idempotency cache lookup failed for key={}: {}", key, ex.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Store a purchase response in the cache. Should be called only AFTER transaction commit (via
   * afterCommit hook).
   *
   * @param key idempotency key
   * @param response the response to cache
   * @param ttl cache duration
   */
  @Override
  public void cache(String key, PurchaseResponse response, Duration ttl) {
    try {
      var json = objectMapper.writeValueAsString(response);
      redisTemplate.opsForValue().set(KEY_PREFIX + key, json, ttl);
    } catch (JsonProcessingException ex) {
      log.warn("Failed to cache idempotency response for key={}: {}", key, ex.getMessage());
    }
  }
}
