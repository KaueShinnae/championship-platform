package com.championship.partidas.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record MatchResultCorrectedPayload(
        UUID matchId,
        UUID championshipId,
        UUID groupId,
        TeamScore previousHome,
        TeamScore previousAway,
        TeamScore correctedHome,
        TeamScore correctedAway,
        boolean wo,
        Instant correctedAt
) {
    public static final String TYPE = "match.result.corrected.v1";
}
