package com.toanlv.flashsale.inventory.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.toanlv.flashsale.common.lock.LeaderLock;
import com.toanlv.flashsale.common.outbox.repository.OutboxEventRepository;

/**
 * Daily cleanup worker for dead letter outbox events.
 *
 * <p>Dead letter events are kept for 7 days for manual inspection, then removed to prevent
 * unbounded table growth.
 *
 * <p>Runs at 02:00 AM daily to avoid peak hours.
 */
@Component
public class DeadLetterCleanupWorker {

  private static final Logger log = LoggerFactory.getLogger(DeadLetterCleanupWorker.class);

  private static final String LOCK_NAME = "dead-letter-cleanup";
  private static final long LOCK_LEASE_SEC = 120L;
  private static final int RETENTION_DAYS = 7;

  private final OutboxEventRepository outboxEventRepository;
  private final LeaderLock leaderLock;

  public DeadLetterCleanupWorker(
      OutboxEventRepository outboxEventRepository, LeaderLock leaderLock) {
    this.outboxEventRepository = outboxEventRepository;
    this.leaderLock = leaderLock;
  }

  @Scheduled(cron = "0 0 2 * * *")
  public void cleanup() {
    leaderLock.runIfLeader(LOCK_NAME, LOCK_LEASE_SEC, this::doCleanup);
  }

  @Transactional
  protected void doCleanup() {
    var cutoff = java.time.Instant.now().minus(RETENTION_DAYS, java.time.temporal.ChronoUnit.DAYS);

    var batch = outboxEventRepository.fetchDeadLetterBatch(500);

    var toDelete = batch.stream().filter(e -> e.getCreatedAt().isBefore(cutoff)).toList();

    if (toDelete.isEmpty()) {
      log.info("Dead letter cleanup: no events older than {} days", RETENTION_DAYS);
      return;
    }

    outboxEventRepository.deleteAll(toDelete);

    log.info(
        "Dead letter cleanup: deleted {} events older than {} days",
        toDelete.size(),
        RETENTION_DAYS);
  }
}
