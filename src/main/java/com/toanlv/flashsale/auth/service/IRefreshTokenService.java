package com.toanlv.flashsale.auth.service;

import com.toanlv.flashsale.auth.service.impl.RefreshTokenService;

import java.util.UUID;

public interface IRefreshTokenService {
    String issue(UUID userId);
    RefreshTokenService.RotationResult rotate(String rawToken);
    void revoke(String rawToken);
}
