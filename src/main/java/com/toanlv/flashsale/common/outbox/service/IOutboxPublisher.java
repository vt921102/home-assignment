package com.toanlv.flashsale.common.outbox.service;

import java.util.Map;
import java.util.UUID;

public interface IOutboxPublisher {
  UUID publish(
      String eventType, String aggregateType, UUID aggregateId, Map<String, Object> payload);
}
