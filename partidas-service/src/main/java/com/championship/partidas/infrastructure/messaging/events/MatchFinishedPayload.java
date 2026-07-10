package com.championship.partidas.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload de match.finished.v1 — ver docs/events/match.finished.v1.md.
 * Dispara o recálculo de classificação no ranking-service.
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
