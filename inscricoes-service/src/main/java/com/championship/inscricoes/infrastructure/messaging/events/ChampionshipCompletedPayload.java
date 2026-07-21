package com.championship.inscricoes.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record ChampionshipCompletedPayload(
        UUID championshipId,
        TeamRef champion,
        Instant completedAt
) {
    public static final String TYPE = "championship.completed.v1";
}
