package com.toanlv.flashsale.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "otp_verifications",
        indexes = {
                @Index(
                        name = "idx_otp_active",
                        columnList = "user_id, purpose"
                ),
                @Index(
                        name = "idx_otp_expires",
                        columnList = "expires_at"
                )
        }
)
public class OtpVerification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "is_used", nullable = false)
    private boolean used = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ----------------------------------------------------------------
    // Factory
    // ----------------------------------------------------------------

    public static OtpVerification create(
            UUID userId,
            String codeHash,
            OtpPurpose purpose,
            Instant expiresAt) {
        var otp = new OtpVerification();
        otp.userId    = userId;
        otp.codeHash  = codeHash;
        otp.purpose   = purpose;
        otp.expiresAt = expiresAt;
        return otp;
    }

    // ----------------------------------------------------------------
    // Business methods
    // ----------------------------------------------------------------

    public void incrementAttempt() {
        this.attemptCount++;
    }

    public void markUsed() {
        this.used = true;
    }

    public boolean isExpired(Clock clock) {
        return Instant.now(clock).isAfter(this.expiresAt);
    }

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------

    public UUID getId()            { return id;           }
    public UUID getUserId()        { return userId;       }
    public String getCodeHash()    { return codeHash;     }
    public OtpPurpose getPurpose() { return purpose;      }
    public int getAttemptCount()   { return attemptCount; }
    public boolean isUsed()        { return used;         }
    public Instant getExpiresAt()  { return expiresAt;    }
    public Instant getCreatedAt()  { return createdAt;    }
}
