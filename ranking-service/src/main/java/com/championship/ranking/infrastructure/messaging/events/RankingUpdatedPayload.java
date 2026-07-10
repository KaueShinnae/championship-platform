package com.championship.ranking.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload de ranking.updated.v1 (produzido) — ver docs/events/ranking.updated.v1.md.
 * Payload enxuto de notificação: quem quiser os dados completos consulta a projeção.
 */
public record RankingUpdatedPayload(
        UUID groupId,
        UUID championshipId,
        Instant updatedAt
) {
    public static final String TYPE = "ranking.updated.v1";
}
