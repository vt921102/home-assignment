package com.toanlv.flashsale.product.repository;


import com.toanlv.flashsale.product.domain.Product;
import com.toanlv.flashsale.product.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    /**
     * Find active products with optional category filter and name search.
     * Uses LEFT JOIN FETCH to avoid N+1 on category.
     */
    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category c
            WHERE p.status = :status
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:search IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.sku)  LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Product> findByFilters(
            @Param("status") ProductStatus status,
            @Param("categoryId") UUID categoryId,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Find product by ID with category eagerly loaded.
     */
    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE p.id = :id
            """)
    Optional<Product> findByIdWithCategory(@Param("id") UUID id);
}
