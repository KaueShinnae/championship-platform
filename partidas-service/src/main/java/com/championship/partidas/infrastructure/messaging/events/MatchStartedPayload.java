package com.championship.partidas.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record MatchStartedPayload(
        UUID matchId,
        UUID championshipId,
        UUID groupId,
        Instant startedAt
) {
    public static final String TYPE = "match.started.v1";
}
