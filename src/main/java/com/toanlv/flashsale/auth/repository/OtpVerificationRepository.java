package com.toanlv.flashsale.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.toanlv.flashsale.auth.domain.OtpPurpose;
import com.toanlv.flashsale.auth.domain.OtpVerification;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

  /** Find the most recent active (unused) OTP for a user and purpose. */
  @Query(
      """
            SELECT o FROM OtpVerification o
             WHERE o.userId  = :userId
               AND o.purpose = :purpose
               AND o.used    = false
             ORDER BY o.createdAt DESC
             LIMIT 1
            """)
  Optional<OtpVerification> findActive(
      @Param("userId") UUID userId, @Param("purpose") OtpPurpose purpose);

  /**
   * Invalidate all active OTPs for a user+purpose. Called before issuing a new OTP to prevent
   * multiple valid codes.
   *
   * @return number of rows updated
   */
  @Modifying
  @Query(
      """
            UPDATE OtpVerification o
               SET o.used = true
             WHERE o.userId  = :userId
               AND o.purpose = :purpose
               AND o.used    = false
            """)
  int invalidateActive(@Param("userId") UUID userId, @Param("purpose") OtpPurpose purpose);
}
