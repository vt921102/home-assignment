package com.toanlv.flashsale.common.outbox.worker;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.toanlv.flashsale.common.lock.LeaderLock;
import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;
import com.toanlv.flashsale.common.outbox.handler.OutboxEventHandler;
import com.toanlv.flashsale.common.outbox.repository.OutboxEventRepository;

/**
 * Scheduled worker that polls the outbox_events table and dispatches each event to its registered
 * handler.
 *
 * <p>Design: - Leader-elected: only one instance runs at a time via LeaderLock. FOR UPDATE SKIP
 * LOCKED in the query also supports multi-worker scale-out if the leader lock is removed in the
 * future. - Each event is processed in its own REQUIRES_NEW transaction so that one failing event
 * does not roll back the entire batch. - Exponential backoff on failure; dead-lettered after
 * MAX_RETRY_ATTEMPTS. - Handler routing via Map<eventType, handler> built at startup from all
 * OutboxEventHandler beans — no switch statement, open/closed.
 */
@Component
public class OutboxDispatchWorker {

  private static final Logger log = LoggerFactory.getLogger(OutboxDispatchWorker.class);

  private static final String LOCK_NAME = "outbox-dispatcher";
  private static final long LOCK_LEASE_SEC = 30L;
  private static final int BATCH_SIZE = 100;
  private static final int MAX_RETRY_ATTEMPTS = 5;

  private final OutboxEventRepository repository;
  private final LeaderLock leaderLock;
  private final Map<String, OutboxEventHandler> handlers;

  public OutboxDispatchWorker(
      OutboxEventRepository repository,
      LeaderLock leaderLock,
      List<OutboxEventHandler> handlerList) {
    this.repository = repository;
    this.leaderLock = leaderLock;
    this.handlers =
        handlerList.stream()
            .collect(Collectors.toMap(OutboxEventHandler::supportedType, Function.identity()));

    log.info("OutboxDispatchWorker initialized with handlers: {}", this.handlers.keySet());
  }

  /**
   * Poll and process pending events every second. Fixed delay ensures the next poll starts only
   * after the current batch is fully processed, preventing overlap.
   */
  @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
  public void dispatch() {
    leaderLock.runIfLeader(LOCK_NAME, LOCK_LEASE_SEC, this::processBatch);
  }

  // ----------------------------------------------------------------
  // Private
  // ----------------------------------------------------------------

  private void processBatch() {
    var events = fetchPendingBatch();
    if (events.isEmpty()) return;

    log.debug("Processing outbox batch of {} events", events.size());

    for (var event : events) {
      processOne(event);
    }
  }

  /**
   * Fetch pending events in a read transaction. FOR UPDATE SKIP LOCKED is in the query — needs an
   * active transaction.
   */
  @Transactional
  protected List<OutboxEvent> fetchPendingBatch() {
    return repository.fetchPendingBatch(BATCH_SIZE);
  }

  /**
   * Process a single event in its own independent transaction. REQUIRES_NEW ensures: - Commit on
   * success is independent of other events - Rollback on failure does not affect already-processed
   * events
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  protected void processOne(OutboxEvent event) {
    var handler = handlers.get(event.getEventType());

    if (handler == null) {
      log.warn(
          "No handler registered for event type [{}], id={}. " + "Marking as dead letter.",
          event.getEventType(),
          event.getId());
      event.markDeadLetter();
      repository.save(event);
      return;
    }

    try {
      handler.handle(event);
      event.markCompleted();
      log.debug("Event [{}] id={} processed successfully", event.getEventType(), event.getId());
    } catch (Exception ex) {
      handleFailure(event, ex);
    } finally {
      repository.save(event);
    }
  }

  private void handleFailure(OutboxEvent event, Exception ex) {
    if (event.isExhausted(MAX_RETRY_ATTEMPTS)) {
      event.markDeadLetter();
      log.error(
          "Event [{}] id={} exhausted {} retries. " + "Moving to dead letter. Last error: {}",
          event.getEventType(),
          event.getId(),
          MAX_RETRY_ATTEMPTS,
          ex.getMessage());
    } else {
      event.scheduleRetry();
      log.warn(
          "Event [{}] id={} failed (attempt {}). " + "Next retry at {}. Error: {}",
          event.getEventType(),
          event.getId(),
          event.getRetryCount(),
          event.getNextRetryAt(),
          ex.getMessage());
    }
  }
}
