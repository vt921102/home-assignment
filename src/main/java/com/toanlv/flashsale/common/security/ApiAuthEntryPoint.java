package com.toanlv.flashsale.common.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Returns 401 JSON response when unauthenticated request hits a protected endpoint. Prevents Spring
 * Security default redirect to /login.
 */
@Component
@RequiredArgsConstructor
public class ApiAuthEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            objectMapper.writeValueAsString(
                Map.of(
                    "status",
                    401,
                    "error",
                    "Unauthorized",
                    "message",
                    "Authentication required. " + "Please provide a valid Bearer token.",
                    "path",
                    request.getRequestURI(),
                    "timestamp",
                    Instant.now().toString())));
  }
}
