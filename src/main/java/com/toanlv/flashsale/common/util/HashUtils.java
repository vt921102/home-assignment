package com.toanlv.flashsale.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Cryptographic utility methods.
 *
 * SHA-256 is used for OTP and refresh token hashing.
 * BCrypt (via PasswordEncoder bean) is used for passwords — not here.
 *
 * Why SHA-256 for OTP (not BCrypt):
 *   - OTP has a short TTL (5 min) and is inherently brute-force-resistant
 *     via attempt limits and rate limiting — BCrypt's slowness is not needed.
 *   - BCrypt cost=12 takes ~150ms per hash. Hashing on every verify request
 *     under 500 TPS would add unnecessary CPU load.
 *   - SHA-256 + per-user salt + global pepper provides sufficient security
 *     for short-lived tokens.
 *
 * Why SHA-256 for refresh tokens:
 *   - Refresh tokens are random UUID — high entropy, no dictionary attack risk.
 *   - SHA-256 is sufficient and fast.
 */
public final class HashUtils {

    private static final String SHA_256 = "SHA-256";

    private HashUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Compute SHA-256 hex digest of the given input.
     *
     * @param input raw string to hash
     * @return lowercase hex string (64 chars)
     */
    public static String sha256(String input) {
        return sha256Bytes(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Compute SHA-256 hex digest of the given bytes.
     *
     * @param input raw bytes to hash
     * @return lowercase hex string (64 chars)
     */
    public static String sha256(byte[] input) {
        return sha256Bytes(input);
    }

    /**
     * Hash OTP with per-user salt and global pepper.
     * Format: SHA-256( otp + ":" + userId + ":" + pepper )
     *
     * Salt (userId) — ensures same OTP produces different hash per user.
     * Pepper (server secret) — ensures hash is uncrackable even with DB dump.
     *
     * @param otp    raw 6-digit OTP code
     * @param userId user UUID as string (salt)
     * @param pepper server-side secret from config (pepper)
     * @return lowercase hex SHA-256 hash
     */
    public static String hashOtp(String otp, String userId, String pepper) {
        var input = otp + ":" + userId + ":" + pepper;
        return sha256(input);
    }

    /**
     * Hash a refresh token for safe storage in DB.
     * Format: SHA-256( rawToken )
     *
     * @param rawToken UUID-based refresh token
     * @return lowercase hex SHA-256 hash
     */
    public static String hashRefreshToken(String rawToken) {
        return sha256(rawToken);
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     *
     * Standard String.equals() short-circuits on first mismatch,
     * leaking information about how many characters matched.
     * MessageDigest.isEqual() always compares all bytes.
     *
     * Use this when comparing hashed secrets (OTP, token hash).
     *
     * @param a first string
     * @param b second string
     * @return true if both strings are equal
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ----------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------

    private static String sha256Bytes(byte[] input) {
        try {
            var digest = MessageDigest.getInstance(SHA_256);
            var hash   = digest.digest(input);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the Java spec — this cannot happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
