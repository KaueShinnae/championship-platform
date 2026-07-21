package com.championship.ranking.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record MatchFinishedPayload(
        UUID matchId,
        UUID championshipId,
        UUID groupId,
        TeamScore homeTeam,
        TeamScore awayTeam,
        boolean wo,
        Instant playedAt
) {
    public static final String TYPE = "match.finished.v1";
}
