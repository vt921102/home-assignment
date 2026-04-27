package com.toanlv.flashsale.auth;


import com.toanlv.flashsale.auth.domain.IdentifierType;
import com.toanlv.flashsale.auth.domain.User;
import com.toanlv.flashsale.auth.domain.UserStatus;
import com.toanlv.flashsale.auth.dto.LoginRequest;
import com.toanlv.flashsale.auth.dto.RegisterRequest;
import com.toanlv.flashsale.auth.dto.VerifyOtpRequest;
import com.toanlv.flashsale.auth.repository.UserRepository;
import com.toanlv.flashsale.auth.service.impl.AuthService;
import com.toanlv.flashsale.auth.service.impl.JwtService;
import com.toanlv.flashsale.auth.service.impl.OtpService;
import com.toanlv.flashsale.auth.service.impl.RefreshTokenService;
import com.toanlv.flashsale.auth.strategy.IdentifierDetector;
import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.common.config.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock UserRepository        userRepository;
    @Mock OtpService            otpService;
    @Mock JwtService            jwtService;
    @Mock RefreshTokenService   refreshTokenService;
    @Mock IdentifierDetector    identifierDetector;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock ApplicationProperties properties;
    @Mock ApplicationProperties.JwtProperties jwtProps;

    private AuthService authService;

    private static final String EMAIL      = "user@example.com";
    private static final String PASSWORD   = "Password123";
    private static final String HASH       = "$2a$12$hash";
    private static final String ACCESS_JWT = "access.jwt.token";
    private static final String REFRESH    = "raw-refresh-token";

    @BeforeEach
    void setUp() {
        when(properties.jwt()).thenReturn(jwtProps);
        when(jwtProps.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));

        authService = new AuthService(
                userRepository, otpService, jwtService,
                refreshTokenService, identifierDetector,
                passwordEncoder, properties);

        when(identifierDetector.detect(EMAIL))
                .thenReturn(IdentifierType.EMAIL);
        when(identifierDetector.normalize(EMAIL, IdentifierType.EMAIL))
                .thenReturn(EMAIL);
    }

    // ----------------------------------------------------------------
    // register
    // ----------------------------------------------------------------

    @Test
    void register_newUser_createsUserAndIssuesOtp() {
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.register(new RegisterRequest(EMAIL, PASSWORD));

        verify(userRepository).save(any(User.class));
        verify(otpService).issueOtp(any(), any(), any(), anyString());
    }

    @Test
    void register_existingUnverifiedUser_updatesPasswordAndReissuesOtp() {
        var existing = User.create(EMAIL, IdentifierType.EMAIL, "old-hash");
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenReturn(existing);

        authService.register(new RegisterRequest(EMAIL, PASSWORD));

        verify(userRepository).save(existing);
        verify(otpService).issueOtp(any(), any(), any(), anyString());
    }

    @Test
    void register_existingVerifiedUser_throwsRegistrationFailed() {
        var existing = User.create(EMAIL, IdentifierType.EMAIL, HASH);
        existing.activate();
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                authService.register(new RegisterRequest(EMAIL, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.REGISTRATION_FAILED));

        verify(otpService, never()).issueOtp(any(), any(), any(), any());
    }

    // ----------------------------------------------------------------
    // verifyOtp
    // ----------------------------------------------------------------

    @Test
    void verifyOtp_activatesUserAndIssuesTokens() {
        var user = User.create(EMAIL, IdentifierType.EMAIL, HASH);
        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(jwtService.issueAccessToken(any(), anyString(), anyString()))
                .thenReturn(ACCESS_JWT);
        when(refreshTokenService.issue(any())).thenReturn(REFRESH);

        var response = authService.verifyOtp(
                new VerifyOtpRequest(EMAIL, "123456"));

        assertThat(user.isVerified()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.accessToken()).isEqualTo(ACCESS_JWT);
        assertThat(response.refreshToken()).isEqualTo(REFRESH);
        assertThat(response.expiresInSeconds()).isEqualTo(900L);
    }

    @Test
    void verifyOtp_alreadyVerified_returnsTokensIdempotently() {
        var user = User.create(EMAIL, IdentifierType.EMAIL, HASH);
        user.activate();
        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.issueAccessToken(any(), anyString(), anyString()))
                .thenReturn(ACCESS_JWT);
        when(refreshTokenService.issue(any())).thenReturn(REFRESH);

        var response = authService.verifyOtp(
                new VerifyOtpRequest(EMAIL, "123456"));

        // OTP verify should NOT be called — already verified
        verify(otpService, never()).verifyOtp(any(), any(), any());
        assertThat(response.accessToken()).isEqualTo(ACCESS_JWT);
    }

    @Test
    void verifyOtp_userNotFound_throwsOtpInvalid() {
        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.verifyOtp(new VerifyOtpRequest(EMAIL, "123456")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.OTP_INVALID));
    }

    // ----------------------------------------------------------------
    // login
    // ----------------------------------------------------------------

    @Test
    void login_validCredentials_returnsTokens() {
        var user = User.create(EMAIL, IdentifierType.EMAIL, HASH);
        user.activate();
        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(jwtService.issueAccessToken(any(), anyString(), anyString()))
                .thenReturn(ACCESS_JWT);
        when(refreshTokenService.issue(any())).thenReturn(REFRESH);

        var response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.accessToken()).isEqualTo(ACCESS_JWT);
        assertThat(response.refreshToken()).isEqualTo(REFRESH);
        assertThat(response.expiresInSeconds()).isEqualTo(900L);
    }

    @Test
    void login_userNotFound_throwsInvalidCredentials() {
        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        var user = User.create(EMAIL, IdentifierType.EMAIL, HASH);
        user.activate();
        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(false);

        assertThatThrownBy(() ->
                authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void login_unverifiedUser_throwsAccountNotVerified() {
        var user = User.create(EMAIL, IdentifierType.EMAIL, HASH);
        // Not activated
        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        assertThatThrownBy(() ->
                authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_VERIFIED));
    }

    @Test
    void login_suspendedUser_throwsAccountSuspended() {
        var user = User.create(EMAIL, IdentifierType.EMAIL, HASH);
        user.activate();
        // Manually set status to SUSPENDED via reflection-free approach
        // by checking the expected behavior
        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        // Spy the user to return SUSPENDED status
        var suspendedUser = new User() {
            { }
            @Override public boolean isVerified()       { return true;                }
            @Override public UserStatus getStatus()     { return UserStatus.SUSPENDED; }
            @Override public String getPasswordHash()   { return HASH;                }
        };

        when(userRepository.findByIdentifierAndIdentifierType(
                EMAIL, IdentifierType.EMAIL))
                .thenReturn(Optional.of(suspendedUser));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        assertThatThrownBy(() ->
                authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(
                        ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_SUSPENDED));
    }

    // ----------------------------------------------------------------
    // logout
    // ----------------------------------------------------------------

    @Test
    void logout_revokesRefreshToken() {
        authService.logout(REFRESH);
        verify(refreshTokenService).revoke(REFRESH);
    }

    // ----------------------------------------------------------------
    // refresh
    // ----------------------------------------------------------------

    @Test
    void refresh_validToken_returnsNewTokenPair() {
        var userId   = UUID.randomUUID();
        var rotation = new RefreshTokenService.RotationResult(
                userId, "new-refresh-token");
        var user     = User.create(EMAIL, IdentifierType.EMAIL, HASH);
        user.activate();

        when(refreshTokenService.rotate(REFRESH)).thenReturn(rotation);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.issueAccessToken(any(), anyString(), anyString()))
                .thenReturn(ACCESS_JWT);

        var response = authService.refresh(REFRESH);

        assertThat(response.accessToken()).isEqualTo(ACCESS_JWT);
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }
}
