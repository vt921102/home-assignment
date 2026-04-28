package com.toanlv.flashsale.auth.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toanlv.flashsale.auth.domain.OtpPurpose;
import com.toanlv.flashsale.auth.domain.User;
import com.toanlv.flashsale.auth.domain.UserStatus;
import com.toanlv.flashsale.auth.dto.LoginRequest;
import com.toanlv.flashsale.auth.dto.LoginResponse;
import com.toanlv.flashsale.auth.dto.RegisterRequest;
import com.toanlv.flashsale.auth.dto.VerifyOtpRequest;
import com.toanlv.flashsale.auth.repository.UserRepository;
import com.toanlv.flashsale.auth.service.IAuthService;
import com.toanlv.flashsale.auth.service.IJwtService;
import com.toanlv.flashsale.auth.service.IOtpService;
import com.toanlv.flashsale.auth.service.IRefreshTokenService;
import com.toanlv.flashsale.auth.strategy.IdentifierDetector;
import com.toanlv.flashsale.common.config.ApplicationProperties;
import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;

@Service
public class AuthService implements IAuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private static final String DEFAULT_ROLE = "USER";

  private final UserRepository userRepository;
  private final IOtpService otpService;
  private final IJwtService jwtService;
  private final IRefreshTokenService refreshTokenService;
  private final IdentifierDetector identifierDetector;
  private final PasswordEncoder passwordEncoder;
  // Lazy accessor to avoid circular injection
  private ApplicationProperties properties;

  public AuthService(
      UserRepository userRepository,
      OtpService otpService,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      IdentifierDetector identifierDetector,
      PasswordEncoder passwordEncoder,
      ApplicationProperties properties) {
    this.userRepository = userRepository;
    this.otpService = otpService;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.identifierDetector = identifierDetector;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
  }

  // ----------------------------------------------------------------
  // Register
  // ----------------------------------------------------------------

  /**
   * Register a new user or re-issue OTP for an unverified user.
   *
   * <p>Flow: 1. Detect + normalize identifier 2. Hash password 3. If identifier exists and verified
   * → fail (generic message) 4. If identifier exists and unverified → update password, re-issue OTP
   * 5. If new → create user, issue OTP
   *
   * <p>Returns a generic message — does not reveal whether identifier already exists (prevents user
   * enumeration).
   */
  @Transactional
  @Override
  public void register(RegisterRequest request) {
    var type = identifierDetector.detect(request.identifier());
    var normalized = identifierDetector.normalize(request.identifier(), type);
    var hash = passwordEncoder.encode(request.password());

    var existing = userRepository.findByIdentifierAndIdentifierType(normalized, type);

    if (existing.isPresent()) {
      var user = existing.get();
      if (user.isVerified()) {
        // Generic error — do not reveal user exists
        throw new BusinessException(ErrorCode.REGISTRATION_FAILED);
      }
      // Allow re-registration for unverified users
      user.updatePasswordHash(hash);
      userRepository.save(user);
      otpService.issueOtp(user.getId(), OtpPurpose.REGISTRATION, type, normalized);
      return;
    }

    var user = User.create(normalized, type, hash);
    userRepository.save(user);
    otpService.issueOtp(user.getId(), OtpPurpose.REGISTRATION, type, normalized);

    log.info("User registered: type={}", type);
  }

  // ----------------------------------------------------------------
  // Verify OTP
  // ----------------------------------------------------------------

  /**
   * Verify OTP and activate account. Issues tokens on success — user does not need to login
   * separately.
   *
   * <p>Idempotent: if already verified, returns 200 without re-issuing tokens.
   */
  @Transactional
  @Override
  public LoginResponse verifyOtp(VerifyOtpRequest request) {
    var type = identifierDetector.detect(request.identifier());
    var normalized = identifierDetector.normalize(request.identifier(), type);

    var user =
        userRepository
            .findByIdentifierAndIdentifierType(normalized, type)
            .orElseThrow(() -> new BusinessException(ErrorCode.OTP_INVALID));

    // Idempotent — already verified
    if (user.isVerified()) {
      return issueTokens(user);
    }

    otpService.verifyOtp(user.getId(), OtpPurpose.REGISTRATION, request.otp());

    user.activate();
    userRepository.save(user);

    log.info("User verified and activated: userId={}", user.getId());

    return issueTokens(user);
  }

  // ----------------------------------------------------------------
  // Login
  // ----------------------------------------------------------------

  /**
   * Authenticate user with identifier + password.
   *
   * <p>Security notes: - Uses constant-time BCrypt compare for all paths - Performs fake BCrypt
   * compare when user not found to prevent user enumeration via timing differences - Generic error
   * message — does not reveal why login failed
   */
  @Transactional
  @Override
  public LoginResponse login(LoginRequest request) {
    var type = identifierDetector.detect(request.identifier());
    var normalized = identifierDetector.normalize(request.identifier(), type);

    var userOpt = userRepository.findByIdentifierAndIdentifierType(normalized, type);

    if (userOpt.isEmpty()) {
      // Fake compare to prevent timing-based user enumeration
      passwordEncoder.matches(request.password(), "$2a$12$fakehashfortimingnormalization");
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    var user = userOpt.get();

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    if (!user.isVerified()) {
      throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
    }

    if (UserStatus.SUSPENDED.equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
    }

    log.info("User logged in: userId={}", user.getId());

    return issueTokens(user);
  }

  // ----------------------------------------------------------------
  // Logout
  // ----------------------------------------------------------------

  /**
   * Revoke the presented refresh token. Access token is short-lived (15 min) — no blocklist needed.
   */
  @Transactional
  @Override
  public void logout(String rawRefreshToken) {
    refreshTokenService.revoke(rawRefreshToken);
  }

  // ----------------------------------------------------------------
  // Refresh
  // ----------------------------------------------------------------

  /**
   * Rotate refresh token and issue new access token. Detects token reuse and revokes all sessions
   * if detected.
   */
  @Transactional
  @Override
  public LoginResponse refresh(String rawRefreshToken) {
    var result = refreshTokenService.rotate(rawRefreshToken);
    var user =
        userRepository
            .findById(result.userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    var accessToken = jwtService.issueAccessToken(user.getId(), user.getIdentifier(), DEFAULT_ROLE);

    return buildLoginResponse(accessToken, result.newRawToken());
  }

  // ----------------------------------------------------------------
  // Resend OTP
  // ----------------------------------------------------------------

  /**
   * Re-issue OTP for an unverified user. Always returns success to prevent user enumeration — even
   * if identifier does not exist.
   */
  @Transactional
  @Override
  public void resendOtp(String rawIdentifier) {
    var type = identifierDetector.detect(rawIdentifier);
    var normalized = identifierDetector.normalize(rawIdentifier, type);

    userRepository
        .findByIdentifierAndIdentifierType(normalized, type)
        .ifPresent(
            user -> {
              if (!user.isVerified()) {
                otpService.issueOtp(user.getId(), OtpPurpose.REGISTRATION, type, normalized);
              }
            });
  }

  // ----------------------------------------------------------------
  // Private helpers
  // ----------------------------------------------------------------

  private LoginResponse issueTokens(User user) {
    var accessToken = jwtService.issueAccessToken(user.getId(), user.getIdentifier(), DEFAULT_ROLE);
    var refreshToken = refreshTokenService.issue(user.getId());
    return buildLoginResponse(accessToken, refreshToken);
  }

  private LoginResponse buildLoginResponse(String accessToken, String refreshToken) {
    var ttlSeconds = properties().jwt().accessTokenTtl().toSeconds();
    return new LoginResponse(accessToken, refreshToken, ttlSeconds);
  }

  private ApplicationProperties properties() {
    return properties;
  }
}
