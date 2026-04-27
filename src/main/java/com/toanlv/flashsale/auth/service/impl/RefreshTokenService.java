package com.toanlv.flashsale.auth.service.impl;


import com.toanlv.flashsale.auth.domain.RefreshToken;
import com.toanlv.flashsale.auth.repository.RefreshTokenRepository;
import com.toanlv.flashsale.auth.service.IRefreshTokenService;
import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.common.util.HashUtils;
import com.toanlv.flashsale.common.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService implements IRefreshTokenService {

    private static final Logger log =
            LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final ApplicationProperties  properties;
    private final Clock clock;


    /**
     * Issue a new refresh token for the user.
     * Returns the raw token — store hash in DB, return raw to client.
     */
    @Override
    @Transactional
    public String issue(UUID userId) {
        var rawToken = UUID.randomUUID().toString();
        var hash     = HashUtils.hashRefreshToken(rawToken);
        var expiry   = Instant.now(clock)
                .plus(properties.jwt().refreshTokenTtl());

        repository.save(RefreshToken.create(userId, hash, expiry));

        return rawToken;
    }

    /**
     * Rotate refresh token — revoke current, issue new.
     *
     * Rotation detects token reuse:
     *   If the presented token is already revoked, an attacker has
     *   stolen and reused it. Revoke all tokens for the user.
     *
     * @param rawToken the refresh token presented by the client
     * @return new raw refresh token
     */
    @Override
    @Transactional
    public RotationResult rotate(String rawToken) {
        var hash  = HashUtils.hashRefreshToken(rawToken);
        var token = repository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (token.isRevoked()) {
            // Reuse detected — revoke all sessions for this user
            repository.revokeAllByUserId(token.getUserId());
            log.warn("Refresh token reuse detected for userId={}. "
                    + "All sessions revoked.", token.getUserId());
            throw new BusinessException(
                    ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        if (token.isExpired()) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        // Revoke current token
        token.revoke();
        repository.save(token);

        // Issue new token
        var newRawToken = issue(token.getUserId());

        return new RotationResult(token.getUserId(), newRawToken);
    }

    /**
     * Revoke a single refresh token on logout.
     */
    @Override
    @Transactional
    public void revoke(String rawToken) {
        var hash = HashUtils.hashRefreshToken(rawToken);
        repository.findByTokenHash(hash).ifPresent(token -> {
            token.revoke();
            repository.save(token);
        });
    }

    public record RotationResult(UUID userId, String newRawToken) {}
}
