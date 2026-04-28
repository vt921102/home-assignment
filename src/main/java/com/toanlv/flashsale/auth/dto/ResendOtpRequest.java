package com.toanlv.flashsale.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(@NotBlank(message = "Identifier is required") String identifier) {}
