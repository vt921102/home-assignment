package com.toanlv.flashsale.flashsale.repository;


import com.toanlv.flashsale.flashsale.domain.FlashSaleSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlashSaleSessionRepository
        extends JpaRepository<FlashSaleSession, UUID> {

    /**
     * Find all active sessions for a given date and time window.
     * Used by FlashSaleQueryService to determine current sessions.
     */
    @Query("""
            SELECT s FROM FlashSaleSession s
            WHERE s.active    = true
              AND s.saleDate  = :date
              AND s.startTime <= :time
              AND s.endTime   >  :time
            """)
    List<FlashSaleSession> findActiveSessions(
            @Param("date") LocalDate date,
            @Param("time") LocalTime time);

    /**
     * Find all sessions for a given date — used by admin.
     */
    List<FlashSaleSession> findBySaleDateOrderByStartTimeAsc(
            LocalDate saleDate);
}
