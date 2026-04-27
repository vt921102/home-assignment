package com.toanlv.flashsale.order.repository;

import com.toanlv.flashsale.order.domain.BalanceTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BalanceTransactionRepository
        extends JpaRepository<BalanceTransaction, UUID> {

    Page<BalanceTransaction> findByUserIdOrderByCreatedAtDesc(
            UUID userId,
            Pageable pageable);
}
