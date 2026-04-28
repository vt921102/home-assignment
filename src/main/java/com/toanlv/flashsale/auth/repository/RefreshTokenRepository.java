package com.toanlv.flashsale.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.toanlv.flashsale.auth.domain.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /**
   * Revoke all tokens for a user. Called when token reuse is detected — revoke all sessions.
   *
   * @return number of tokens revoked
   */
  @Modifying
  @Query(
      """
            UPDATE RefreshToken r
               SET r.revoked = true
             WHERE r.userId  = :userId
               AND r.revoked = false
            """)
  int revokeAllByUserId(@Param("userId") UUID userId);

  /** Count active (non-revoked, non-expired) tokens for a user. */
  @Query(
      value =
          """
            SELECT COUNT(*) FROM refresh_tokens
             WHERE user_id  = :userId
               AND revoked  = false
               AND expires_at > NOW()
            """,
      nativeQuery = true)
  long countActiveByUserId(@Param("userId") UUID userId);
}
