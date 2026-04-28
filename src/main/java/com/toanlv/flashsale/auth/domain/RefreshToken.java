package com.toanlv.flashsale.auth.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
      @Index(name = "idx_refresh_token_user", columnList = "user_id"),
      @Index(name = "idx_refresh_token_expires", columnList = "expires_at")
    })
@Getter
public class RefreshToken {

  @Id @GeneratedValue private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private boolean revoked = false;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // ----------------------------------------------------------------
  // Factory
  // ----------------------------------------------------------------

  public static RefreshToken create(UUID userId, String tokenHash, Instant expiresAt) {
    var token = new RefreshToken();
    token.userId = userId;
    token.tokenHash = tokenHash;
    token.expiresAt = expiresAt;
    return token;
  }

  // ----------------------------------------------------------------
  // Business methods
  // ----------------------------------------------------------------

  public void revoke() {
    this.revoked = true;
  }

  public boolean isExpired() {
    return Instant.now().isAfter(this.expiresAt);
  }

  public boolean isValid() {
    return !this.revoked && !isExpired();
  }
}
