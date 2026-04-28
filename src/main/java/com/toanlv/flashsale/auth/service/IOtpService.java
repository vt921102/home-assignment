package com.toanlv.flashsale.auth.service;

import java.util.UUID;

import com.toanlv.flashsale.auth.domain.IdentifierType;
import com.toanlv.flashsale.auth.domain.OtpPurpose;

public interface IOtpService {
  void issueOtp(UUID userId, OtpPurpose purpose, IdentifierType channel, String identifier);

  void verifyOtp(UUID userId, OtpPurpose purpose, String inputOtp);
}
