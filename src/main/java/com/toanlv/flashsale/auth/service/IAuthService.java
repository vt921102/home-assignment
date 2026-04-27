package com.toanlv.flashsale.auth.service;

import com.toanlv.flashsale.auth.dto.LoginRequest;
import com.toanlv.flashsale.auth.dto.LoginResponse;
import com.toanlv.flashsale.auth.dto.RegisterRequest;
import com.toanlv.flashsale.auth.dto.VerifyOtpRequest;

public interface IAuthService {
    void register(RegisterRequest request);
    LoginResponse verifyOtp(VerifyOtpRequest request);
    LoginResponse login(LoginRequest request);
    void logout(String rawRefreshToken);
    LoginResponse refresh(String rawRefreshToken);
    void resendOtp(String rawIdentifier);
}
