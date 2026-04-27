package com.toanlv.flashsale.product.repository;


import com.toanlv.flashsale.product.domain.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository
        extends JpaRepository<ProductCategory, UUID> {

    Optional<ProductCategory> findByName(String name);

    boolean existsByName(String name);

    /**
     * Load all top-level categories (no parent).
     */
    List<ProductCategory> findByParentIsNull();

    /**
     * Load all children of a given parent category.
     */
    List<ProductCategory> findByParentId(UUID parentId);

    /**
     * Load full category tree in a single query using LEFT JOIN FETCH.
     * Avoids N+1 when building the tree structure.
     */
    @Query("""
            SELECT c FROM ProductCategory c
            LEFT JOIN FETCH c.parent
            ORDER BY c.name ASC
            """)
    List<ProductCategory> findAllWithParent();
}
