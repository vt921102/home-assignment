package com.toanlv.flashsale.common.exception;

import lombok.Getter;

/**
 * Domain-level exception that maps to a specific ErrorCode. Thrown by service layer to signal
 * expected business rule violations. Handled by GlobalExceptionHandler — never propagates as 500.
 */
@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, String detail) {
    super(detail);
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }
}
