package com.toanlv.flashsale.order.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.toanlv.flashsale.order.domain.Order;
import com.toanlv.flashsale.order.domain.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

  /**
   * Find orders for a user with items eagerly loaded. Avoids N+1 when rendering order list with
   * item counts.
   */
  @Query(
      """
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.userId = :userId
            ORDER BY o.createdAt DESC
            """)
  Page<Order> findByUserId(@Param("userId") UUID userId, Pageable pageable);

  /** Find a specific order by ID with items eagerly loaded. */
  @Query(
      """
            SELECT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.id = :id
            """)
  Optional<Order> findByIdWithItems(@Param("id") UUID id);

  /** Find order by idempotency key. Used when handling duplicate purchase requests. */
  Optional<Order> findByIdempotencyKey(String idempotencyKey);

  /**
   * Check if a user has a completed order for a specific flash sale session item (source_ref_id).
   * Used for per-item purchase history validation.
   */
  @Query(
      """
            SELECT COUNT(o) > 0 FROM Order o
            JOIN o.items i
            WHERE o.userId  = :userId
              AND i.sourceRefId = :sessionItemId
              AND o.status   = :status
            """)
  boolean existsByUserIdAndSessionItemIdAndStatus(
      @Param("userId") UUID userId,
      @Param("sessionItemId") UUID sessionItemId,
      @Param("status") OrderStatus status);
}
