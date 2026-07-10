package com.championship.partidas.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload de match.started.v1 — ver docs/events/match.started.v1.md.
 * Publicado na transição AGENDADA -> EM_ANDAMENTO.
 */
public record MatchStartedPayload(
        UUID matchId,
        UUID championshipId,
        UUID groupId,
        Instant startedAt
) {
    public static final String TYPE = "match.started.v1";
}
