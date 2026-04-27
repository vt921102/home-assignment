package com.toanlv.flashsale.auth.service;

import com.toanlv.flashsale.auth.domain.IdentifierType;
import com.toanlv.flashsale.auth.domain.OtpPurpose;

import java.util.UUID;

public interface IOtpService {
    void issueOtp(
            UUID userId,
            OtpPurpose purpose,
            IdentifierType channel,
            String identifier);
    void verifyOtp(
            UUID userId,
            OtpPurpose purpose,
            String inputOtp);
}
