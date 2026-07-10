package com.championship.inscricoes.infrastructure.messaging.events;

import java.util.List;
import java.util.UUID;

/**
 * Payload de team.registered.v1 — ver docs/events/team.registered.v1.md.
 */
public record TeamRegisteredPayload(
        UUID teamId,
        String teamName,
        UUID championshipId,
        List<PlayerRef> players
) {
    public static final String TYPE = "team.registered.v1";
}
