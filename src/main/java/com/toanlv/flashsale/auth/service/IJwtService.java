package com.toanlv.flashsale.auth.service;

import java.util.UUID;

public interface IJwtService {
    String issueAccessToken(
            UUID userId,
            String identifier,
            String role);
    UUID extractUserId(String token);
}
