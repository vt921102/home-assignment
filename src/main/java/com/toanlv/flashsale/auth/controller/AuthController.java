package com.toanlv.flashsale.auth.controller;


import com.toanlv.flashsale.auth.dto.LoginRequest;
import com.toanlv.flashsale.auth.dto.LoginResponse;
import com.toanlv.flashsale.auth.dto.LogoutRequest;
import com.toanlv.flashsale.auth.dto.RefreshTokenRequest;
import com.toanlv.flashsale.auth.dto.RegisterRequest;
import com.toanlv.flashsale.auth.dto.ResendOtpRequest;
import com.toanlv.flashsale.auth.dto.VerifyOtpRequest;
import com.toanlv.flashsale.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication",
        description = "Register, verify OTP, login, logout, refresh token")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register new account",
            description = "Accepts email or phone number. Sends OTP for verification.")
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Verify OTP",
            description = "Verify OTP received after registration. Issues tokens on success.")
    @PostMapping("/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @Operation(summary = "Login",
            description = "Authenticate with identifier and password. Returns access + refresh tokens.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Logout",
            description = "Revoke refresh token. Access token expires naturally after 15 minutes.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Refresh access token",
            description = "Exchange refresh token for a new access + refresh token pair.")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "Resend OTP",
            description = "Re-send OTP to the given identifier. Rate limited to 5 per hour.")
    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request.identifier());
        return ResponseEntity.ok().build();
    }
}
