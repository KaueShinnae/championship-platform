package com.championship.inscricoes.infrastructure.messaging;

import jakarta.persistence.*;

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

    protected ProcessedEvent() {
    }

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }
}
