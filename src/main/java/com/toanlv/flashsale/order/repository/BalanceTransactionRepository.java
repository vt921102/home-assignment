package com.toanlv.flashsale.order.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.toanlv.flashsale.order.domain.BalanceTransaction;

@Repository
public interface BalanceTransactionRepository extends JpaRepository<BalanceTransaction, UUID> {

  Page<BalanceTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
