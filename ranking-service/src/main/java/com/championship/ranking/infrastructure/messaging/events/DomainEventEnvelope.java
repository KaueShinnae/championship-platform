package com.championship.ranking.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope padrão de todo evento publicado/consumido no Kafka (ver docs/events/).
 * event_id é a chave de deduplicação usada pelos consumers idempotentes.
 */
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
