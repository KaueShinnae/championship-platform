package com.championship.inscricoes.infrastructure.messaging.events;

import java.util.List;
import java.util.UUID;

/**
 * Payload de championship.permissions.changed.v1 — snapshot completo de quem
 * gerencia o campeonato (dono + admins delegados). Ver
 * docs/events/championship.permissions.changed.v1.md.
 */
public record ChampionshipPermissionsChangedPayload(
        UUID championshipId,
        UUID ownerId,
        List<UUID> adminIds
) {
    public static final String TYPE = "championship.permissions.changed.v1";
}
