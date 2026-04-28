package com.toanlv.flashsale.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
    @NotBlank(message = "Identifier is required") String identifier,
    @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{4,8}$", message = "OTP must be 4-8 digits")
        String otp) {}
