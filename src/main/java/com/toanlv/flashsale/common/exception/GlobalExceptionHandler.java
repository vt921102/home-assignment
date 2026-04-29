package com.toanlv.flashsale.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
@Order()
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
    var error = ApiError.of(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    log.debug("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
    return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    var details =
        ex.getBindingResult().getAllErrors().stream()
            .map(
                e -> {
                  if (e instanceof FieldError fe) {
                    return fe.getField() + ": " + fe.getDefaultMessage();
                  }
                  return e.getObjectName() + ": " + e.getDefaultMessage();
                })
            .sorted()
            .toList();
    var error = ApiError.withDetails(ErrorCode.VALIDATION_FAILED, request.getRequestURI(), details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ApiError> handleMissingHeader(
      MissingRequestHeaderException ex, HttpServletRequest request) {
    var error =
        ApiError.of(
            ErrorCode.VALIDATION_FAILED,
            "Required header '" + ex.getHeaderName() + "' is missing",
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    var error =
        ApiError.of(
            ErrorCode.VALIDATION_FAILED,
            "Request body is malformed or missing",
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    var error =
        ApiError.of(
            ErrorCode.VALIDATION_FAILED,
            "Invalid value for parameter '" + ex.getName() + "'",
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    var error =
        ApiError.of(
            ErrorCode.VALIDATION_FAILED,
            "HTTP method '" + ex.getMethod() + "' is not supported",
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(
      NoHandlerFoundException ex, HttpServletRequest request) {
    var error = ApiError.of(ErrorCode.RESOURCE_NOT_FOUND, request.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiError> handleAuthentication(
      AuthenticationException ex, HttpServletRequest request) {
    var error = ApiError.of(ErrorCode.INVALID_CREDENTIALS, request.getRequestURI());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    var error = ApiError.of(ErrorCode.ACCESS_DENIED, request.getRequestURI());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn(
        "Data integrity violation at [{}]: {}",
        request.getRequestURI(),
        ex.getMostSpecificCause().getMessage());
    var error = ApiError.of(ErrorCode.DUPLICATE_REQUEST, request.getRequestURI());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception at [{}]", request.getRequestURI(), ex);
    var error = ApiError.of(ErrorCode.INTERNAL_ERROR, request.getRequestURI());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
