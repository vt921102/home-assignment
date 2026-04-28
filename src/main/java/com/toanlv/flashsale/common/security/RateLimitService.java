package com.toanlv.flashsale.common.security;

import java.time.Duration;
import java.util.List;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

@Component
public class RateLimitService {

  /**
   * Atomic sliding-window counter via Lua script.
   *
   * <p>INCR key — increment counter PEXPIRE key ms — set TTL only on first increment (prevents TTL
   * reset) Compare vs limit — reject if exceeded
   *
   * <p>Lua executes atomically on Redis side — no race condition between increment and expire
   * across multiple instances.
   */
  private static final String RATE_LIMIT_LUA =
      """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            if current > tonumber(ARGV[2]) then
                return 0
            end
            return 1
            """;

  private final RedissonClient redisson;

  public RateLimitService(RedissonClient redisson) {
    this.redisson = redisson;
  }

  /**
   * @return true if request is allowed, false if rate limit exceeded
   */
  public boolean tryAcquire(String key, int limit, Duration window) {
    var result =
        redisson
            .getScript(StringCodec.INSTANCE)
            .eval(
                RScript.Mode.READ_WRITE,
                RATE_LIMIT_LUA,
                RScript.ReturnType.INTEGER,
                List.of(key),
                String.valueOf(window.toMillis()),
                String.valueOf(limit));
    return Long.valueOf(1L).equals(result);
  }
}
