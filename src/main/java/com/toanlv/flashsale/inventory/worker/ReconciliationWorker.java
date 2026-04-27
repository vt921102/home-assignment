package com.toanlv.flashsale.inventory.worker;


import com.toanlv.flashsale.common.lock.LeaderLock;
import com.toanlv.flashsale.inventory.repository.InventoryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hourly reconciliation worker.
 *
 * Scans for inventory rows where the invariant
 * total = available + reserved is violated.
 *
 * In a correctly functioning system, this should never fire.
 * If it does, it logs an alert and increments a metric for
 * monitoring systems (Prometheus / Grafana) to catch.
 *
 * Does NOT auto-correct — corrections require human review
 * to avoid masking bugs silently.
 */
@Component
public class ReconciliationWorker {

    private static final Logger log =
            LoggerFactory.getLogger(ReconciliationWorker.class);

    private static final String LOCK_NAME      = "inventory-reconciliation";
    private static final long   LOCK_LEASE_SEC = 600L;

    private final InventoryRepository inventoryRepository;
    private final LeaderLock           leaderLock;
    private final MeterRegistry        meterRegistry;

    public ReconciliationWorker(
            InventoryRepository inventoryRepository,
            LeaderLock leaderLock,
            MeterRegistry meterRegistry) {
        this.inventoryRepository = inventoryRepository;
        this.leaderLock          = leaderLock;
        this.meterRegistry       = meterRegistry;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void reconcile() {
        leaderLock.runIfLeader(
                LOCK_NAME,
                LOCK_LEASE_SEC,
                this::doReconcile);
    }

    @Transactional(readOnly = true)
    protected void doReconcile() {
        log.info("Starting inventory reconciliation");

        var mismatches = inventoryRepository.findInconsistent();

        if (mismatches.isEmpty()) {
            log.info("Inventory reconciliation passed — no inconsistencies found");
            return;
        }

        for (var inv : mismatches) {
            log.error(
                    "Inventory inconsistency detected: "
                            + "productId={}, total={}, available={}, reserved={}. "
                            + "Expected: total = available + reserved.",
                    inv.getProductId(),
                    inv.getTotalQuantity(),
                    inv.getAvailableQuantity(),
                    inv.getReservedQuantity()
            );

            meterRegistry.counter(
                    "inventory.reconciliation.mismatch",
                    "productId", inv.getProductId().toString()
            ).increment();
        }

        log.warn("Inventory reconciliation found {} inconsistency(ies). "
                + "Manual review required.", mismatches.size());
    }
}
