package com.toanlv.flashsale.auth.service.impl;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.toanlv.flashsale.auth.service.IOtpGenerator;

@Component
public class OtpGenerator implements IOtpGenerator {

  private final SecureRandom random = new SecureRandom();
  private final int length;

  public OtpGenerator(@Value("${app.otp.length:6}") int length) {
    this.length = length;
  }

  /**
   * Generate a zero-padded numeric OTP of configured length. Uses SecureRandom — cryptographically
   * strong, not predictable.
   *
   * @return e.g. "034521" for length=6
   */
  @Override
  public String generate() {
    int bound = (int) Math.pow(10, length);
    int n = random.nextInt(bound);
    return String.format("%0" + length + "d", n);
  }
}
