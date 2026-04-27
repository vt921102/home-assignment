package com.toanlv.flashsale.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toanlv.flashsale.common.config.ApplicationProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(IdempotencyFilter.class);

    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private static final String PURCHASE_PATH =
            "/api/v1/flash-sale/purchase";

    private static final long   LOCK_TTL_SECONDS = 30;
    private static final String LOCK_VALUE       = "PROCESSING";

    private final StringRedisTemplate redisTemplate;
    private final ApplicationProperties properties;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(
            StringRedisTemplate redisTemplate,
            ApplicationProperties properties,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.properties    = properties;
        this.objectMapper  = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!shouldApply(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        var idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        // Require header for purchase endpoint
        if (!StringUtils.hasText(idempotencyKey)) {
            writeMissingKeyResponse(response);
            return;
        }

        // Validate UUID format
        if (!isValidUuid(idempotencyKey)) {
            writeInvalidKeyResponse(response);
            return;
        }

        var userId = extractUserId();
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Rate limit purchase per user
        var cfg = properties.rateLimit().purchase();
        var rateLimitKey = "rate:purchase:" + userId;
        if (!isWithinRateLimit(rateLimitKey,
                cfg.limit(), cfg.window().toSeconds())) {
            writeRateLimitResponse(response);
            return;
        }

        // Distributed lock to prevent concurrent duplicate requests
        var lockKey = "idempotency:lock:" + userId + ":" + idempotencyKey;
        var acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, LOCK_VALUE,
                        LOCK_TTL_SECONDS, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(acquired)) {
            writeConcurrentRequestResponse(response);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Lock released by PurchaseService.afterCommit() via
            // IdempotencyService. Remove here only if response is error
            // (no commit occurred).
            if (response.getStatus() >= 400) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    private boolean shouldApply(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && PURCHASE_PATH.equals(request.getServletPath());
    }

    private String extractUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId().toString();
        }
        return null;
    }

    private boolean isWithinRateLimit(
            String key, int limit, long windowSeconds) {
        var count = redisTemplate.opsForValue().increment(key);
        if (count == null) return true;
        if (count == 1L) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count <= limit;
    }

    private boolean isValidUuid(String value) {
        try {
            java.util.UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void writeMissingKeyResponse(HttpServletResponse response)
            throws IOException {
        writeJson(response, HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Header X-Idempotency-Key is required for this endpoint");
    }

    private void writeInvalidKeyResponse(HttpServletResponse response)
            throws IOException {
        writeJson(response, HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "X-Idempotency-Key must be a valid UUID v4");
    }

    private void writeRateLimitResponse(HttpServletResponse response)
            throws IOException {
        writeJson(response, HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "Purchase rate limit exceeded. Please slow down.");
    }

    private void writeConcurrentRequestResponse(HttpServletResponse response)
            throws IOException {
        writeJson(response, HttpStatus.CONFLICT.value(),
                "Conflict",
                "A request with this idempotency key is already being processed");
    }

    private void writeJson(
            HttpServletResponse response,
            int status,
            String error,
            String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(Map.of(
                        "status",    status,
                        "error",     error,
                        "message",   message,
                        "timestamp", Instant.now().toString()
                ))
        );
    }
}
