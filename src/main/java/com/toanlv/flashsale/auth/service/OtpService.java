package com.toanlv.flashsale.auth.service;

import com.toanlv.flashsale.auth.domain.IdentifierType;
import com.toanlv.flashsale.auth.domain.OtpPurpose;
import com.toanlv.flashsale.auth.domain.OtpVerification;
import com.toanlv.flashsale.auth.repository.OtpVerificationRepository;
import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.common.outbox.service.OutboxPublisher;
import com.toanlv.flashsale.common.security.RateLimitService;
import com.toanlv.flashsale.common.util.HashUtils;
import com.toanlv.flashsale.common.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class OtpService {

    private static final Logger log =
            LoggerFactory.getLogger(OtpService.class);

    private final OtpVerificationRepository repository;
    private final OutboxPublisher           outboxPublisher;
    private final OtpGenerator              generator;
    private final RateLimitService          rateLimitService;
    private final ApplicationProperties     properties;
    private final Clock                     clock;

    public OtpService(
            OtpVerificationRepository repository,
            OutboxPublisher outboxPublisher,
            OtpGenerator generator,
            RateLimitService rateLimitService,
            ApplicationProperties properties,
            Clock clock) {
        this.repository      = repository;
        this.outboxPublisher = outboxPublisher;
        this.generator       = generator;
        this.rateLimitService = rateLimitService;
        this.properties      = properties;
        this.clock           = clock;
    }

    /**
     * Issue a new OTP for the user.
     * Invalidates previous active OTPs for the same user+purpose.
     * Publishes OTP_DISPATCH event to outbox for async mock delivery.
     *
     * Must be called within an active transaction
     * (OutboxPublisher requires Propagation.MANDATORY).
     */
    @Transactional
    public void issueOtp(
            UUID userId,
            OtpPurpose purpose,
            IdentifierType channel,
            String identifier) {

        var cfg = properties.otp();

        // Rate limit per identifier
        var rateLimitKey = "otp:resend:" + identifier;
        if (!rateLimitService.tryAcquire(
                rateLimitKey,
                cfg.resendLimit(),
                cfg.resendWindow())) {
            throw new BusinessException(ErrorCode.OTP_RESEND_LIMIT_EXCEEDED);
        }

        // Invalidate previous OTPs for this user+purpose
        repository.invalidateActive(userId, purpose);

        // Generate and hash
        var rawOtp   = generator.generate();
        var codeHash = HashUtils.hashOtp(
                rawOtp, userId.toString(), cfg.pepper());
        var expiresAt = Instant.now(clock).plus(cfg.ttl());

        // Save hashed OTP
        repository.save(OtpVerification.create(
                userId, codeHash, purpose, expiresAt));

        // Publish to outbox (same transaction)
        outboxPublisher.publish(
                "OTP_DISPATCH",
                "USER",
                userId,
                Map.of(
                        "channel",    channel.name(),
                        "identifier", identifier,
                        "otp",        rawOtp,
                        "purpose",    purpose.name(),
                        "expiresAt",  expiresAt.toString()
                )
        );

        log.info("OTP issued for userId={} purpose={}", userId, purpose);
    }

    /**
     * Verify OTP submitted by user.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param userId   user attempting verification
     * @param purpose  expected OTP purpose
     * @param inputOtp raw OTP string from user
     * @throws BusinessException on any verification failure
     */
    @Transactional
    public void verifyOtp(
            UUID userId,
            OtpPurpose purpose,
            String inputOtp) {

        var cfg = properties.otp();

        var otp = repository.findActive(userId, purpose)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OTP_INVALID));

        // Check max attempts first — prevent further guessing
        if (otp.getAttemptCount() >= cfg.maxAttempts()) {
            otp.markUsed();
            repository.save(otp);
            throw new BusinessException(
                    ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
        }

        // Check expiry
        if (otp.isExpired(clock)) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }

        // Check already used
        if (otp.isUsed()) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        // Constant-time compare
        var inputHash = HashUtils.hashOtp(
                inputOtp, userId.toString(), cfg.pepper());
        if (!HashUtils.constantTimeEquals(inputHash, otp.getCodeHash())) {
            otp.incrementAttempt();
            repository.save(otp);
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        // Success — mark used
        otp.markUsed();
        repository.save(otp);

        log.info("OTP verified for userId={} purpose={}", userId, purpose);
    }
}
