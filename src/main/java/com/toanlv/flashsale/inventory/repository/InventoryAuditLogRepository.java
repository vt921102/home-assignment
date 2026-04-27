package com.toanlv.flashsale.inventory.repository;


import com.toanlv.flashsale.inventory.domain.InventoryAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryAuditLogRepository
        extends JpaRepository<InventoryAuditLog, UUID> {

    Page<InventoryAuditLog> findByProductIdOrderByCreatedAtDesc(
            UUID productId,
            Pageable pageable);

    boolean existsBySourceEventId(UUID sourceEventId);
}
