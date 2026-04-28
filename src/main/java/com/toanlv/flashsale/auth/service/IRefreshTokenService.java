package com.toanlv.flashsale.auth.service;

import java.util.UUID;

import com.toanlv.flashsale.auth.service.impl.RefreshTokenService;

public interface IRefreshTokenService {
  String issue(UUID userId);

  RefreshTokenService.RotationResult rotate(String rawToken);

  void revoke(String rawToken);
}
