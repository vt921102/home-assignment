package com.toanlv.flashsale.flashsale.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.toanlv.flashsale.flashsale.domain.FlashSaleSessionItem;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface FlashSaleSessionItemRepository extends JpaRepository<FlashSaleSessionItem, UUID> {

  /**
   * Find all active session items for the current time window. Eagerly fetches session and product
   * to avoid N+1. Used by FlashSaleQueryService for the public listing endpoint.
   */
  @Query(
      """
            SELECT i FROM FlashSaleSessionItem i
            JOIN FETCH i.session s
            JOIN FETCH i.product p
            LEFT JOIN FETCH p.category
            WHERE s.active    = true
              AND s.saleDate  = :date
              AND s.startTime <= :time
              AND s.endTime   >  :time
              AND i.soldQuantity < i.totalQuantity
            ORDER BY p.name ASC
            """)
  List<FlashSaleSessionItem> findActiveItems(
      @Param("date") LocalDate date, @Param("time") LocalTime time);

  /**
   * Load a session item with its session and product for purchase. PESSIMISTIC_WRITE (exclusive
   * lock) serializes concurrent purchases to guarantee correct version reads for decrementSold.
   *
   * <p>Lock timeout 3000ms — fail fast rather than queue threads.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
  @Query(
      """
            SELECT i FROM FlashSaleSessionItem i
            JOIN FETCH i.session
            JOIN FETCH i.product
            WHERE i.id = :id
            """)
  Optional<FlashSaleSessionItem> findByIdForPurchase(@Param("id") UUID id);

  /**
   * Atomic stock decrement using optimistic concurrency.
   *
   * <p>WHERE version = :version — detects concurrent modification. WHERE soldQuantity <
   * totalQuantity — prevents oversell at DB level.
   *
   * <p>Returns 1 if update succeeded, 0 if version mismatch or out of stock. Caller retries via
   * RetryTemplate on 0 result.
   */
  @Modifying
  @Query(
      value =
          """
            UPDATE flash_sale_session_items
               SET sold_quantity = sold_quantity + 1,
                   version       = version + 1
             WHERE id            = :id
               AND version       = :version
               AND sold_quantity < total_quantity
            """,
      nativeQuery = true)
  int decrementSold(@Param("id") UUID id, @Param("version") long version);

  /** Find items for a specific session — used by admin. */
  @Query(
      """
            SELECT i FROM FlashSaleSessionItem i
            JOIN FETCH i.product p
            LEFT JOIN FETCH p.category
            WHERE i.session.id = :sessionId
            ORDER BY p.name ASC
            """)
  List<FlashSaleSessionItem> findBySessionId(@Param("sessionId") UUID sessionId);
}
