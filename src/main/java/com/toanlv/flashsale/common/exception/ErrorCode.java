package com.toanlv.flashsale.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {

  // ----------------------------------------------------------------
  // Auth — 1xxx
  // ----------------------------------------------------------------
  IDENTIFIER_REQUIRED(1001, "Identifier is required", HttpStatus.BAD_REQUEST),
  IDENTIFIER_INVALID(
      1002, "Identifier must be a valid email or Vietnamese phone number", HttpStatus.BAD_REQUEST),
  REGISTRATION_FAILED(1003, "Registration failed", HttpStatus.BAD_REQUEST),
  USER_NOT_FOUND(1004, "User not found", HttpStatus.NOT_FOUND),
  INVALID_CREDENTIALS(1005, "Invalid credentials", HttpStatus.UNAUTHORIZED),
  ACCOUNT_NOT_VERIFIED(1006, "Account is not verified", HttpStatus.FORBIDDEN),
  ACCOUNT_SUSPENDED(1007, "Account is suspended", HttpStatus.FORBIDDEN),

  // ----------------------------------------------------------------
  // OTP — 11xx
  // ----------------------------------------------------------------
  OTP_INVALID(1101, "Invalid or expired OTP", HttpStatus.BAD_REQUEST),
  OTP_EXPIRED(1102, "OTP has expired", HttpStatus.BAD_REQUEST),
  OTP_ALREADY_USED(1103, "OTP has already been used", HttpStatus.BAD_REQUEST),
  OTP_MAX_ATTEMPTS_EXCEEDED(1104, "Maximum OTP attempts exceeded", HttpStatus.TOO_MANY_REQUESTS),
  OTP_RESEND_LIMIT_EXCEEDED(
      1105, "OTP resend limit exceeded. Please try again later", HttpStatus.TOO_MANY_REQUESTS),

  // ----------------------------------------------------------------
  // Token — 12xx
  // ----------------------------------------------------------------
  TOKEN_INVALID(1201, "Invalid token", HttpStatus.UNAUTHORIZED),
  TOKEN_EXPIRED(1202, "Token has expired", HttpStatus.UNAUTHORIZED),
  REFRESH_TOKEN_NOT_FOUND(1203, "Refresh token not found", HttpStatus.UNAUTHORIZED),
  REFRESH_TOKEN_REVOKED(1204, "Refresh token has been revoked", HttpStatus.UNAUTHORIZED),
  REFRESH_TOKEN_REUSE_DETECTED(
      1205, "Token reuse detected. All sessions have been invalidated", HttpStatus.UNAUTHORIZED),

  // ----------------------------------------------------------------
  // Product — 2xxx
  // ----------------------------------------------------------------
  PRODUCT_NOT_FOUND(2001, "Product not found", HttpStatus.NOT_FOUND),
  PRODUCT_ALREADY_EXISTS(2002, "Product with this SKU already exists", HttpStatus.CONFLICT),
  PRODUCT_INACTIVE(2003, "Product is not active", HttpStatus.BAD_REQUEST),
  CATEGORY_NOT_FOUND(2004, "Category not found", HttpStatus.NOT_FOUND),

  // ----------------------------------------------------------------
  // Flash Sale — 3xxx
  // ----------------------------------------------------------------
  SESSION_NOT_FOUND(3001, "Flash sale session not found", HttpStatus.NOT_FOUND),
  SESSION_ITEM_NOT_FOUND(3002, "Flash sale session item not found", HttpStatus.NOT_FOUND),
  FLASH_SALE_NOT_ACTIVE(3003, "Flash sale is not active at this time", HttpStatus.BAD_REQUEST),
  OUT_OF_STOCK(3004, "Product is out of stock", HttpStatus.CONFLICT),
  DAILY_LIMIT_EXCEEDED(
      3005, "You have already purchased a flash sale item today", HttpStatus.CONFLICT),
  INSUFFICIENT_BALANCE(
      3006, "Insufficient balance to complete this purchase", HttpStatus.PAYMENT_REQUIRED),
  SESSION_TIME_CONFLICT(
      3007, "Session time overlaps with an existing session", HttpStatus.CONFLICT),

  // ----------------------------------------------------------------
  // Order — 4xxx
  // ----------------------------------------------------------------
  ORDER_NOT_FOUND(4001, "Order not found", HttpStatus.NOT_FOUND),
  ORDER_ACCESS_DENIED(4002, "You do not have access to this order", HttpStatus.FORBIDDEN),
  ORDER_ALREADY_COMPLETED(4003, "Order has already been completed", HttpStatus.CONFLICT),
  ORDER_CANNOT_BE_CANCELLED(
      4004, "Order cannot be cancelled in its current state", HttpStatus.BAD_REQUEST),

  // ----------------------------------------------------------------
  // Inventory — 5xxx
  // ----------------------------------------------------------------
  INVENTORY_NOT_FOUND(5001, "Inventory record not found", HttpStatus.NOT_FOUND),
  INVENTORY_INSUFFICIENT(5002, "Insufficient inventory", HttpStatus.CONFLICT),
  INVENTORY_INCONSISTENCY(
      5003, "Inventory inconsistency detected", HttpStatus.INTERNAL_SERVER_ERROR),

  // ----------------------------------------------------------------
  // General — 9xxx
  // ----------------------------------------------------------------
  VALIDATION_FAILED(9001, "Validation failed", HttpStatus.BAD_REQUEST),
  RESOURCE_NOT_FOUND(9002, "Resource not found", HttpStatus.NOT_FOUND),
  ACCESS_DENIED(9003, "Access denied", HttpStatus.FORBIDDEN),
  DUPLICATE_REQUEST(9004, "Duplicate request detected", HttpStatus.CONFLICT),
  RATE_LIMIT_EXCEEDED(9005, "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
  INTERNAL_ERROR(9006, "An internal error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

  private final int code;
  private final String message;
  private final HttpStatus httpStatus;

  ErrorCode(int code, String message, HttpStatus httpStatus) {
    this.code = code;
    this.message = message;
    this.httpStatus = httpStatus;
  }
}
