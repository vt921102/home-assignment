package com.toanlv.flashsale.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, long expiresInSeconds) {}
