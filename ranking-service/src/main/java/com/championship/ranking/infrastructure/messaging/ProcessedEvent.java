package com.championship.ranking.infrastructure.messaging;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de eventos já processados por este serviço (idempotência de consumer).
 * Antes de processar uma mensagem, checar se event_id já existe aqui.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "aggregate_id")
    private UUID aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(name = "trace_id", length = 32)
    private String traceId;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public ProcessedEvent(UUID eventId, String eventType, UUID aggregateId) {
        this(eventId);
        this.eventType = eventType;
        this.aggregateId = aggregateId;
    }

    public ProcessedEvent(UUID eventId, String eventType, UUID aggregateId, String payload, String traceId) {
        this(eventId, eventType, aggregateId);
        this.payload = payload;
        this.traceId = traceId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public String getTraceId() {
        return traceId;
    }
}
