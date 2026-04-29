package com.toanlv.flashsale.product.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.toanlv.flashsale.product.domain.Product;
import com.toanlv.flashsale.product.domain.ProductStatus;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

  Optional<Product> findBySku(String sku);

  boolean existsBySku(String sku);

  /**
   * Find products by filters with pagination.
   *
   * <p>JOIN FETCH removed — causes HHH90003004 warning with Pageable and is rejected by Spring Data
   * JPA 3.5 strict sort validation. Category is loaded lazily per product when needed for DTO
   * mapping.
   *
   * <p>countQuery provided explicitly to avoid Spring Data trying to derive count query from the
   * main query.
   */
  @Query(
      value =
          """
            SELECT p FROM Product p
            WHERE p.status = :status
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (CAST(:search AS String) IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                   OR LOWER(p.sku)  LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))
            """,
      countQuery =
          """
            SELECT COUNT(p) FROM Product p
            WHERE p.status = :status
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (CAST(:search AS String) IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                   OR LOWER(p.sku)  LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))
            """)
  Page<Product> findByFilters(
      @Param("status") ProductStatus status,
      @Param("categoryId") UUID categoryId,
      @Param("search") String search,
      Pageable pageable);

  @Query(
      """
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE p.id = :id
            """)
  Optional<Product> findByIdWithCategory(@Param("id") UUID id);
}
