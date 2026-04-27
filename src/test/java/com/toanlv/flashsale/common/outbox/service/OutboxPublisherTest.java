package com.toanlv.flashsale.common.outbox.service;

import org.junit.jupiter.api.Test;


import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;
import com.toanlv.flashsale.common.outbox.domain.OutboxStatus;
import com.toanlv.flashsale.common.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    OutboxEventRepository repository;

    @InjectMocks
    OutboxPublisher publisher;

    @Test
    void publish_savesEventWithCorrectFields() {
        var eventId     = UUID.randomUUID();
        var aggregateId = UUID.randomUUID();
        var payload     = Map.<String, Object>of("key", "value");

        var savedEvent = OutboxEvent.create(
                "OTP_DISPATCH", "USER", aggregateId, payload);

        when(repository.save(any())).thenAnswer(inv -> {
            var e = (OutboxEvent) inv.getArgument(0);
            return e;
        });

        // Use reflection or spy to return known ID
        when(repository.save(any())).thenReturn(savedEvent);

        publisher.publish("OTP_DISPATCH", "USER", aggregateId, payload);

        var captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());

        var captured = captor.getValue();
        assertThat(captured.getEventType()).isEqualTo("OTP_DISPATCH");
        assertThat(captured.getAggregateType()).isEqualTo("USER");
        assertThat(captured.getAggregateId()).isEqualTo(aggregateId);
        assertThat(captured.getPayload()).isEqualTo(payload);
        assertThat(captured.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(captured.getRetryCount()).isZero();
    }
}