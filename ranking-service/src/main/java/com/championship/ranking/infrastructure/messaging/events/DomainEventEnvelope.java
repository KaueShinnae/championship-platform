package com.championship.ranking.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record DomainEventEnvelope<T>(
        UUID eventId,
        Instant occurredAt,
        UUID aggregateId,
        String type,
        T payload
) {
    public static <T> DomainEventEnvelope<T> of(UUID aggregateId, String type, T payload) {
        return new DomainEventEnvelope<>(UUID.randomUUID(), Instant.now(), aggregateId, type, payload);
    }
}
