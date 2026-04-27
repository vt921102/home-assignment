package com.toanlv.flashsale.auth.service.impl;


import com.toanlv.flashsale.auth.service.IJwtService;
import com.toanlv.flashsale.common.config.ApplicationProperties;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService implements IJwtService {

    private final PrivateKey privateKey;
    private final JwtParser  jwtParser;
    private final ApplicationProperties properties;
    private final Clock clock;

    /**
     * Issue a signed RS256 JWT access token.
     *
     * Claims:
     *   sub         — userId
     *   identifier  — email or phone
     *   role        — USER or ADMIN
     *   iss         — issuer from config
     *   iat         — issued at
     *   exp         — expiry
     */
    @Override
    public String issueAccessToken(
            UUID userId,
            String identifier,
            String role) {
        var now    = Instant.now(clock);
        var expiry = now.plus(properties.jwt().accessTokenTtl());

        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of(
                        "identifier", identifier,
                        "role",       role
                ))
                .issuer(properties.jwt().issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(privateKey)
                .compact();
    }

    /**
     * Extract userId from a valid access token.
     *
     * @throws io.jsonwebtoken.JwtException if token is invalid or expired
     */
    @Override
    public UUID extractUserId(String token) {
        var subject = jwtParser
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return UUID.fromString(subject);
    }
}
