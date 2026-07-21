package com.championship.ranking.infrastructure.messaging;

import com.championship.ranking.infrastructure.messaging.events.DomainEventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DomainEventWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final TraceIdProvider traceIdProvider;

    public DomainEventWriter(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper,
                             TraceIdProvider traceIdProvider) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.traceIdProvider = traceIdProvider;
    }

    public <T> void write(UUID aggregateId, String type, T payload) {
        DomainEventEnvelope<T> envelope = DomainEventEnvelope.of(aggregateId, type, payload);
        try {
            String json = objectMapper.writeValueAsString(envelope);
            outboxEventRepository.save(new OutboxEvent(
                    envelope.eventId(), aggregateId, type, json, envelope.occurredAt(),
                    traceIdProvider.currentTraceId()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("falha ao serializar evento " + type, e);
        }
    }
}
