package com.toanlv.flashsale.auth;


import com.toanlv.flashsale.auth.domain.IdentifierType;
import com.toanlv.flashsale.auth.domain.OtpPurpose;
import com.toanlv.flashsale.auth.domain.OtpVerification;
import com.toanlv.flashsale.auth.repository.OtpVerificationRepository;
import com.toanlv.flashsale.auth.service.impl.OtpGenerator;
import com.toanlv.flashsale.auth.service.impl.OtpService;
import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.common.outbox.service.OutboxPublisher;
import com.toanlv.flashsale.common.security.RateLimitService;
import com.toanlv.flashsale.common.util.HashUtils;
import com.toanlv.flashsale.common.config.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpServiceTest {

    @Mock OtpVerificationRepository otpRepo;
    @Mock OutboxPublisher            outboxPublisher;
    @Mock OtpGenerator               generator;
    @Mock RateLimitService           rateLimitService;
    @Mock ApplicationProperties      properties;
    @Mock ApplicationProperties.OtpProperties otpProps;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);

    private static final String PEPPER  = "test-pepper";
    private static final String RAW_OTP = "123456";

    private OtpService otpService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        when(properties.otp()).thenReturn(otpProps);
        when(otpProps.pepper()).thenReturn(PEPPER);
        when(otpProps.ttl()).thenReturn(Duration.ofMinutes(5));
        when(otpProps.maxAttempts()).thenReturn(5);
        when(otpProps.resendLimit()).thenReturn(5);
        when(otpProps.resendWindow()).thenReturn(Duration.ofHours(1));

        otpService = new OtpService(
                otpRepo, outboxPublisher, generator,
                rateLimitService, properties, clock);

        userId = UUID.randomUUID();
    }

    // ----------------------------------------------------------------
    // issueOtp
    // ----------------------------------------------------------------

    @Test
    void issueOtp_invalidatesPreviousOtp_savesNew_andPublishesEvent() {
        when(rateLimitService.tryAcquire(anyString(), anyInt(), any()))
                .thenReturn(true);
        when(generator.generate()).thenReturn(RAW_OTP);
        when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        otpService.issueOtp(userId, OtpPurpose.REGISTRATION,
                IdentifierType.EMAIL, "user@example.com");

        verify(otpRepo).invalidateActive(userId, OtpPurpose.REGISTRATION);

        var otpCaptor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(otpRepo).save(otpCaptor.capture());

        var saved = otpCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getPurpose()).isEqualTo(OtpPurpose.REGISTRATION);
        assertThat(saved.getCodeHash()).isEqualTo(
                HashUtils.hashOtp(RAW_OTP, userId.toString(), PEPPER));
        assertThat(saved.getExpiresAt()).isAfter(Instant.now(clock));

        var payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(outboxPublisher).publish(
                eq("OTP_DISPATCH"), eq("USER"), eq(userId),
                payloadCaptor.capture());

        var payload = payloadCaptor.getValue();
        assertThat(payload.get("otp")).isEqualTo(RAW_OTP);
        assertThat(payload.get("channel")).isEqualTo("EMAIL");
        assertThat(payload.get("purpose")).isEqualTo("REGISTRATION");
    }

    @Test
    void issueOtp_throwsRateLimitExceeded_whenLimitReached() {
        when(rateLimitService.tryAcquire(anyString(), anyInt(), any()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                otpService.issueOtp(userId, OtpPurpose.REGISTRATION,
                        IdentifierType.EMAIL, "user@example.com"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.OTP_RESEND_LIMIT_EXCEEDED));

        verify(otpRepo, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any());
    }

    // ----------------------------------------------------------------
    // verifyOtp — success
    // ----------------------------------------------------------------

    @Test
    void verifyOtp_success_marksOtpAsUsed() {
        var otp = buildValidOtp(userId, RAW_OTP);
        when(otpRepo.findActive(userId, OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(otp));

        otpService.verifyOtp(userId, OtpPurpose.REGISTRATION, RAW_OTP);

        assertThat(otp.isUsed()).isTrue();
        verify(otpRepo).save(otp);
    }

    // ----------------------------------------------------------------
    // verifyOtp — failure cases
    // ----------------------------------------------------------------

    @Test
    void verifyOtp_throwsInvalid_whenOtpNotFound() {
        when(otpRepo.findActive(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                otpService.verifyOtp(userId, OtpPurpose.REGISTRATION, "000000"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.OTP_INVALID));
    }

    @Test
    void verifyOtp_throwsInvalid_whenWrongCode() {
        var otp = buildValidOtp(userId, RAW_OTP);
        when(otpRepo.findActive(any(), any())).thenReturn(Optional.of(otp));

        assertThatThrownBy(() ->
                otpService.verifyOtp(userId, OtpPurpose.REGISTRATION, "000000"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.OTP_INVALID));

        assertThat(otp.getAttemptCount()).isEqualTo(1);
        assertThat(otp.isUsed()).isFalse();
    }

    @Test
    void verifyOtp_throwsExpired_whenOtpExpired() {
        var otp = OtpVerification.create(
                userId,
                HashUtils.hashOtp(RAW_OTP, userId.toString(), PEPPER),
                OtpPurpose.REGISTRATION,
                Instant.now(clock).minusSeconds(1));

        when(otpRepo.findActive(any(), any())).thenReturn(Optional.of(otp));

        assertThatThrownBy(() ->
                otpService.verifyOtp(userId, OtpPurpose.REGISTRATION, RAW_OTP))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.OTP_EXPIRED));
    }

    @Test
    void verifyOtp_throwsMaxAttempts_andInvalidatesOtp_whenAttemptsExceeded() {
        var otp = buildValidOtp(userId, RAW_OTP);
        // Simulate 5 previous failed attempts
        for (int i = 0; i < 5; i++) otp.incrementAttempt();

        when(otpRepo.findActive(any(), any())).thenReturn(Optional.of(otp));

        assertThatThrownBy(() ->
                otpService.verifyOtp(userId, OtpPurpose.REGISTRATION, RAW_OTP))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED));

        assertThat(otp.isUsed()).isTrue();
        verify(otpRepo).save(otp);
    }

    @Test
    void verifyOtp_throwsInvalid_whenAlreadyUsed() {
        var otp = buildValidOtp(userId, RAW_OTP);
        otp.markUsed();
        when(otpRepo.findActive(any(), any())).thenReturn(Optional.of(otp));

        assertThatThrownBy(() ->
                otpService.verifyOtp(userId, OtpPurpose.REGISTRATION, RAW_OTP))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.OTP_INVALID));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private OtpVerification buildValidOtp(UUID userId, String rawOtp) {
        return OtpVerification.create(
                userId,
                HashUtils.hashOtp(rawOtp, userId.toString(), PEPPER),
                OtpPurpose.REGISTRATION,
                Instant.now(clock).plusSeconds(300));
    }
}
