package com.championship.partidas.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload de championship.completed.v1 — ver docs/events/championship.completed.v1.md.
 * Publicado quando o campeão é definido (final do mata-mata ou última rodada
 * dos pontos corridos); o inscricoes-service consome e encerra o campeonato.
 */
public record ChampionshipCompletedPayload(
        UUID championshipId,
        TeamRef champion,
        Instant completedAt
) {
    public static final String TYPE = "championship.completed.v1";
}
