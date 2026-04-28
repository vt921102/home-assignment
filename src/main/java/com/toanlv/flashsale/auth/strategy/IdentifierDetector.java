package com.toanlv.flashsale.auth.strategy;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.toanlv.flashsale.auth.domain.IdentifierType;
import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;

@Component
public final class IdentifierDetector {

  private static final Pattern EMAIL = Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

  private static final Pattern PHONE = Pattern.compile("^(\\+84|84|0)[3|5|7|8|9][0-9]{8}$");

  /**
   * Detect whether the identifier is an email or phone number.
   *
   * @param input raw identifier string from request
   * @return detected IdentifierType
   * @throws BusinessException if input is blank or matches neither pattern
   */
  public IdentifierType detect(String input) {
    return switch (input) {
      case null -> throw new BusinessException(ErrorCode.IDENTIFIER_REQUIRED);
      case String s when s.isBlank() -> throw new BusinessException(ErrorCode.IDENTIFIER_REQUIRED);
      case String s when EMAIL.matcher(s.trim()).matches() -> IdentifierType.EMAIL;
      case String s when PHONE.matcher(s.trim()).matches() -> IdentifierType.PHONE;
      default -> throw new BusinessException(ErrorCode.IDENTIFIER_INVALID);
    };
  }

  /**
   * Normalize identifier for consistent storage. Email: lowercase + trim Phone: normalize to
   * 0xxxxxxxxx format
   *
   * @param input raw identifier
   * @param type detected type
   * @return normalized identifier
   */
  public String normalize(String input, IdentifierType type) {
    return switch (type) {
      case EMAIL -> input.trim().toLowerCase();
      case PHONE -> normalizePhone(input.trim());
    };
  }

  private String normalizePhone(String phone) {
    if (phone.startsWith("+84")) return "0" + phone.substring(3);
    if (phone.startsWith("84")) return "0" + phone.substring(2);
    return phone;
  }
}
