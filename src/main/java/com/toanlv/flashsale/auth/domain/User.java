package com.toanlv.flashsale.auth.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;

@Entity
@Table(
    name = "users",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_users_identifier",
            columnNames = {"identifier", "identifier_type"}),
    indexes = @Index(name = "idx_users_status", columnList = "status"))
@Getter
public class User {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false, length = 150)
  private String identifier;

  @Enumerated(EnumType.STRING)
  @Column(name = "identifier_type", nullable = false, length = 10)
  private IdentifierType identifierType;

  @Column(name = "password_hash", nullable = false, length = 60)
  private String passwordHash;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserStatus status = UserStatus.PENDING_VERIFICATION;

  @Column(name = "is_verified", nullable = false)
  private boolean verified = false;

  /**
   * User role — determines access level. USER : regular user, can browse and purchase flash sale
   * items. ADMIN : can manage products, sessions, inventory.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role = UserRole.USER;

  @Version private Long version;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // ----------------------------------------------------------------
  // Factory
  // ----------------------------------------------------------------

  public static User create(String identifier, IdentifierType identifierType, String passwordHash) {
    var user = new User();
    user.identifier = identifier;
    user.identifierType = identifierType;
    user.passwordHash = passwordHash;
    user.status = UserStatus.PENDING_VERIFICATION;
    user.verified = false;
    user.role = UserRole.USER;
    return user;
  }

  // ----------------------------------------------------------------
  // Business methods
  // ----------------------------------------------------------------

  public void activate() {
    this.verified = true;
    this.status = UserStatus.ACTIVE;
  }

  public void updatePasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void promoteToAdmin() {
    this.role = UserRole.ADMIN;
  }

  public boolean isActive() {
    return UserStatus.ACTIVE.equals(this.status) && this.verified;
  }

  public boolean isAdmin() {
    return UserRole.ADMIN.equals(this.role);
  }
}
