package com.toanlv.flashsale.common.outbox.handler;

import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;

/**
 * Strategy interface for processing outbox events.
 *
 * <p>Each implementation handles exactly one event_type. Spring collects all implementations and
 * the OutboxDispatchWorker routes events to the correct handler via a Map keyed on supportedType().
 *
 * <p>To add a new event type: 1. Create a class implementing OutboxEventHandler 2. Annotate
 * with @Component 3. Return the new event_type string from supportedType() No other changes
 * required — the worker auto-discovers it.
 *
 * <p>Implementations: - auth/outbox/OtpDispatchHandler → "OTP_DISPATCH" -
 * inventory/outbox/PurchaseSyncHandler → "FLASH_SALE_PURCHASED" -
 * inventory/outbox/RestockSyncHandler → "PRODUCT_RESTOCKED"
 */
public interface OutboxEventHandler {

  /**
   * @return the event_type string this handler processes
   */
  String supportedType();

  /**
   * Process the event. Must be idempotent — may be called more than once for the same event if the
   * worker retries after a partial failure.
   *
   * @param event the outbox event to process
   * @throws Exception any exception triggers retry logic in the worker
   */
  void handle(OutboxEvent event) throws Exception;
}
