package com.toanlv.flashsale.flashsale.repository;


import com.toanlv.flashsale.flashsale.domain.UserDailyPurchaseLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface UserDailyPurchaseLimitRepository
        extends JpaRepository<UserDailyPurchaseLimit, UUID> {

    /**
     * Atomic insert using INSERT ON CONFLICT DO NOTHING.
     *
     * Enforces the "1 user = 1 purchase per day" rule atomically.
     * UNIQUE constraint on (user_id, purchase_date) is the DB guard.
     *
     * Returns 1 if inserted (first purchase today), 0 if already exists.
     * Caller throws DAILY_LIMIT_EXCEEDED if 0 is returned.
     *
     * This is safe under concurrent requests — PostgreSQL serializes
     * at the constraint level, not at the application level.
     */
    @Modifying
    @Query(value = """
            INSERT INTO user_daily_purchase_limits
                (id, user_id, purchase_date, purchase_count)
            VALUES
                (gen_random_uuid(), :userId, :date, 1)
            ON CONFLICT (user_id, purchase_date)
            DO NOTHING
            """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("userId") UUID userId,
            @Param("date") LocalDate date);
}
