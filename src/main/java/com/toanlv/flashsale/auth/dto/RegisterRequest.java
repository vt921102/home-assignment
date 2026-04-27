package com.toanlv.flashsale.auth.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Identifier is required")
        @Size(max = 150, message = "Identifier must not exceed 150 characters")
        String identifier,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {}
