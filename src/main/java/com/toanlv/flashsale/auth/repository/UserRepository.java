package com.toanlv.flashsale.auth.repository;


import com.toanlv.flashsale.auth.domain.IdentifierType;
import com.toanlv.flashsale.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdentifierAndIdentifierType(
            String identifier,
            IdentifierType identifierType);

    boolean existsByIdentifierAndIdentifierType(
            String identifier,
            IdentifierType identifierType);

    /**
     * Atomic balance deduction.
     * WHERE balance >= :amount prevents negative balance.
     *
     * @return 1 if updated, 0 if insufficient balance
     */
    @Modifying
    @Query(value = """
            UPDATE users
               SET balance    = balance - :amount,
                   updated_at = NOW()
             WHERE id      = :userId
               AND balance >= :amount
            """,
            nativeQuery = true)
    int deductBalance(
            @Param("userId") UUID userId,
            @Param("amount") BigDecimal amount);

    @Query("SELECT u.balance FROM User u WHERE u.id = :userId")
    Optional<BigDecimal> findBalanceById(@Param("userId") UUID userId);
}
