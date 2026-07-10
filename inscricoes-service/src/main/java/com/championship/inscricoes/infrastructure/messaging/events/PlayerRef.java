package com.championship.inscricoes.infrastructure.messaging.events;

import java.util.UUID;

public record PlayerRef(UUID playerId, String name) {
}
