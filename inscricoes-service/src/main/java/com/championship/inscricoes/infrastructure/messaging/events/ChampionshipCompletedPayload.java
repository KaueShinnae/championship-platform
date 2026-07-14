package com.championship.inscricoes.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload de championship.completed.v1 — ver docs/events/championship.completed.v1.md.
 * Publicado pelo partidas-service quando o campeão é definido; este serviço
 * consome e encerra o campeonato registrando o campeão.
 */
public record ChampionshipCompletedPayload(
        UUID championshipId,
        TeamRef champion,
        Instant completedAt
) {
    public static final String TYPE = "championship.completed.v1";
}
