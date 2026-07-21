package com.championship.partidas.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record MatchScheduledPayload(
        UUID matchId,
        UUID championshipId,
        UUID groupId,
        TeamRef homeTeam,
        TeamRef awayTeam,
        Instant scheduledAt,
        String local
) {
    public static final String TYPE = "match.scheduled.v1";
}
