package com.championship.ranking.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload de match.finished.v1 (consumido) — ver docs/events/match.finished.v1.md.
 * Contrato espelhado do produtor (partidas-service); mudança incompatível
 * exige um .v2, nunca alterar este.
 */
public record MatchFinishedPayload(
        UUID matchId,
        UUID championshipId,
        UUID groupId,
        TeamScore homeTeam,
        TeamScore awayTeam,
        Instant playedAt
) {
    public static final String TYPE = "match.finished.v1";
}
