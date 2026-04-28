package com.toanlv.flashsale.common.outbox.service.impl;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;
import com.toanlv.flashsale.common.outbox.repository.OutboxEventRepository;
import com.toanlv.flashsale.common.outbox.service.IOutboxPublisher;

/// Transactional outbox publisher.
/// Publishes events into the outbox_events table within the caller's
/// existing transaction. This guarantees that the business state change
/// and the event are committed atomically — either both persist or neither does.
/// Usage:
///   Called from PurchaseService, OtpService, etc. inside their
///   @Transactional method — MANDATORY propagation enforces this.
/// Why Propagation.MANDATORY:
///   If OutboxPublisher is called outside a transaction (programmer error),
///   Spring throws IllegalTransactionStateException immediately at startup
///   rather than silently publishing an event without a surrounding transaction,
///   which could lead to events without corresponding business data.
@Service
public class OutboxPublisher implements IOutboxPublisher {

  private final OutboxEventRepository repository;

  public OutboxPublisher(OutboxEventRepository repository) {
    this.repository = repository;
  }

  /**
   * Publish an outbox event within the current transaction.
   *
   * @param eventType business event name, e.g. "OTP_DISPATCH"
   * @param aggregateType domain entity type, e.g. "USER"
   * @param aggregateId primary key of the aggregate entity
   * @param payload event-specific data
   * @return ID of the saved outbox event
   */
  @Transactional(propagation = Propagation.MANDATORY)
  @Override
  public UUID publish(
      String eventType, String aggregateType, UUID aggregateId, Map<String, Object> payload) {

    var event = OutboxEvent.create(eventType, aggregateType, aggregateId, payload);

    return repository.save(event).getId();
  }
}
