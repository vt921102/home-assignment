package com.toanlv.flashsale.inventory.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.toanlv.flashsale.inventory.domain.Inventory;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

  Optional<Inventory> findByProductId(UUID productId);

  /**
   * Atomic decrement of available_quantity and increment of reserved_quantity in a single UPDATE.
   *
   * <p>WHERE available_quantity >= :qty ensures stock never goes negative. Returns 1 if updated, 0
   * if insufficient stock.
   *
   * <p>This is the database-level guard for inventory consistency.
   */
  @Modifying
  @Query(
      value =
          """
            UPDATE inventories
               SET available_quantity = available_quantity - :qty,
                   reserved_quantity  = reserved_quantity  + :qty,
                   version            = version + 1,
                   updated_at         = NOW()
             WHERE product_id          = :productId
               AND available_quantity >= :qty
            """,
      nativeQuery = true)
  int decrementAvailable(@Param("productId") UUID productId, @Param("qty") int qty);

  /** Atomic increment of available_quantity and total_quantity for restocking a product. */
  @Modifying
  @Query(
      value =
          """
            UPDATE inventories
               SET available_quantity = available_quantity + :qty,
                   total_quantity     = total_quantity     + :qty,
                   version            = version + 1,
                   updated_at         = NOW()
             WHERE product_id = :productId
            """,
      nativeQuery = true)
  int incrementAvailable(@Param("productId") UUID productId, @Param("qty") int qty);

  /** Atomic release of reserved stock back to available. Used on order refund or cancellation. */
  @Modifying
  @Query(
      value =
          """
            UPDATE inventories
               SET available_quantity = available_quantity + :qty,
                   reserved_quantity  = reserved_quantity  - :qty,
                   version            = version + 1,
                   updated_at         = NOW()
             WHERE product_id         = :productId
               AND reserved_quantity >= :qty
            """,
      nativeQuery = true)
  int releaseReserved(@Param("productId") UUID productId, @Param("qty") int qty);

  /**
   * Find all inventories where the invariant is violated. Used by reconciliation worker.
   * total_quantity != available_quantity + reserved_quantity OR any column is negative.
   */
  @Query(
      value =
          """
            SELECT *
              FROM inventories
             WHERE total_quantity != available_quantity + reserved_quantity
                OR available_quantity < 0
                OR reserved_quantity  < 0
                OR total_quantity     < 0
            """,
      nativeQuery = true)
  List<Inventory> findInconsistent();
}
