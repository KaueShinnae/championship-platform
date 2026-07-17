package com.championship.partidas.infrastructure.messaging.events;

import java.util.List;
import java.util.UUID;

/**
 * Payload de championship.permissions.changed.v1 (publicado pelo
 * inscricoes-service) — snapshot completo de dono + admins do campeonato.
 * Ver docs/events/championship.permissions.changed.v1.md.
 */
public record ChampionshipPermissionsChangedPayload(
        UUID championshipId,
        UUID ownerId,
        List<UUID> adminIds
) {
    public static final String TYPE = "championship.permissions.changed.v1";
}
