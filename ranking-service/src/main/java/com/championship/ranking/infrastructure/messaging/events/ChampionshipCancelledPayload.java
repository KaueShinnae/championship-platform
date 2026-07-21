package com.championship.ranking.infrastructure.messaging.events;

import java.util.UUID;

public record ChampionshipCancelledPayload(
        UUID championshipId
) {
    public static final String TYPE = "championship.cancelled.v1";
}
