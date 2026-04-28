package com.toanlv.flashsale.common.exception;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform error response body returned by GlobalExceptionHandler.
 *
 * <p>Shape: { "status": 400, "code": 1001, "error": "Bad Request", "message": "Identifier is
 * required", "path": "/api/v1/auth/register", "timestamp": "2026-04-27T10:00:00Z", "details":
 * ["field: must not be blank"] // only on validation errors }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    int status,
    int code,
    String error,
    String message,
    String path,
    Instant timestamp,
    List<String> details) {

  // ----------------------------------------------------------------
  // Factory methods
  // ----------------------------------------------------------------

  public static ApiError of(ErrorCode errorCode, String path) {
    return new ApiError(
        errorCode.getHttpStatus().value(),
        errorCode.getCode(),
        errorCode.getHttpStatus().getReasonPhrase(),
        errorCode.getMessage(),
        path,
        Instant.now(),
        null);
  }

  public static ApiError of(ErrorCode errorCode, String detail, String path) {
    return new ApiError(
        errorCode.getHttpStatus().value(),
        errorCode.getCode(),
        errorCode.getHttpStatus().getReasonPhrase(),
        detail,
        path,
        Instant.now(),
        null);
  }

  public static ApiError withDetails(ErrorCode errorCode, String path, List<String> details) {
    return new ApiError(
        errorCode.getHttpStatus().value(),
        errorCode.getCode(),
        errorCode.getHttpStatus().getReasonPhrase(),
        errorCode.getMessage(),
        path,
        Instant.now(),
        details);
  }
}
