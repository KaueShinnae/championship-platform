package com.championship.partidas.infrastructure.messaging.events;

import java.util.List;
import java.util.UUID;

public record ChampionshipPermissionsChangedPayload(
        UUID championshipId,
        UUID ownerId,
        List<UUID> adminIds
) {
    public static final String TYPE = "championship.permissions.changed.v1";
}
