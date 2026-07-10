package com.championship.partidas.infrastructure.messaging.events;

import java.util.UUID;

public record TeamRef(UUID teamId, String name) {
}
