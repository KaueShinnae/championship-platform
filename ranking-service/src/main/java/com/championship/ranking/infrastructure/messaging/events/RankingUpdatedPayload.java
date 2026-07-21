package com.championship.ranking.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record RankingUpdatedPayload(
        UUID groupId,
        UUID championshipId,
        Instant updatedAt
) {
    public static final String TYPE = "ranking.updated.v1";
}
