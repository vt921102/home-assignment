package com.toanlv.flashsale.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
        @Valid @NotNull JwtProperties jwt,
        @Valid @NotNull OtpProperties otp,
        @Valid @NotNull RateLimitProperties rateLimit,
        @Valid @NotNull FlashSaleProperties flashSale
) {

    public record JwtProperties(
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl,
            @NotBlank String keystorePath,
            @NotBlank String keystorePassword,
            @NotBlank String keyAlias
    ) {}

    public record OtpProperties(
            @Min(4) @Max(8) int length,
            @NotNull Duration ttl,
            @NotBlank String pepper,
            @Min(1) @Max(10) int maxAttempts,
            @Min(1) int resendLimit,
            @NotNull Duration resendWindow
    ) {}

    public record RateLimitProperties(
            @Valid @NotNull RateLimitWindow login,
            @Valid @NotNull RateLimitWindow otp,
            @Valid @NotNull RateLimitWindow purchase
    ) {
        public record RateLimitWindow(
                @Min(1) int limit,
                @NotNull Duration window
        ) {}
    }

    public record FlashSaleProperties(
            @NotNull Duration cacheTtl,
            @Valid @NotNull PurchaseRetryProperties purchaseRetry
    ) {
        public record PurchaseRetryProperties(
                @Min(1) @Max(10) int maxAttempts,
                @NotNull Duration initialDelay,
                @DecimalMin("1.0") double multiplier
        ) {}
    }
}
