package com.toanlv.flashsale.common.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toanlv.flashsale.common.config.ApplicationProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final RateLimitService rateLimitService;
  private final ApplicationProperties properties;
  private final ObjectMapper objectMapper;

  public RateLimitFilter(
      RateLimitService rateLimitService,
      ApplicationProperties properties,
      ObjectMapper objectMapper) {
    this.rateLimitService = rateLimitService;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    var path = request.getServletPath();
    var ip = extractClientIp(request);

    if (isLoginEndpoint(path)) {
      var cfg = properties.rateLimit().login();
      if (!rateLimitService.tryAcquire("rate:login:" + ip, cfg.limit(), cfg.window())) {
        writeRateLimitResponse(response, "Too many login attempts. Please try again later.");
        return;
      }
    }

    if (isOtpEndpoint(path)) {
      var cfg = properties.rateLimit().otp();
      // OTP rate limit keyed by identifier from request body is
      // handled in OtpService. This filter covers IP-level throttle.
      if (!rateLimitService.tryAcquire("rate:otp:ip:" + ip, cfg.limit(), cfg.window())) {
        writeRateLimitResponse(response, "Too many OTP requests. Please try again later.");
        return;
      }
    }

    if (isPurchaseEndpoint(path)) {
      // Purchase rate limit is user-scoped, handled after JWT auth.
      // See IdempotencyFilter for user-scoped purchase limit.
    }

    filterChain.doFilter(request, response);
  }

  private boolean isLoginEndpoint(String path) {
    return "/api/v1/auth/login".equals(path);
  }

  private boolean isOtpEndpoint(String path) {
    return "/api/v1/auth/resend-otp".equals(path) || "/api/v1/auth/verify-otp".equals(path);
  }

  private boolean isPurchaseEndpoint(String path) {
    return "/api/v1/flash-sale/purchase".equals(path);
  }

  private String extractClientIp(HttpServletRequest request) {
    var forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      // X-Forwarded-For may contain multiple IPs: client, proxy1, proxy2
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private void writeRateLimitResponse(HttpServletResponse response, String message)
      throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            objectMapper.writeValueAsString(
                Map.of(
                    "status",
                    429,
                    "error",
                    "Too Many Requests",
                    "message",
                    message,
                    "timestamp",
                    Instant.now().toString())));
  }
}
