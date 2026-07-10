package com.championship.partidas.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload de match.scheduled.v1 — ver docs/events/match.scheduled.v1.md.
 */
public record MatchScheduledPayload(
        UUID matchId,
        UUID championshipId,
        UUID groupId,
        TeamRef homeTeam,
        TeamRef awayTeam,
        Instant scheduledAt
) {
    public static final String TYPE = "match.scheduled.v1";
}
