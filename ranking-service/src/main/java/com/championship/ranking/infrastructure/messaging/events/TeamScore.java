package com.championship.ranking.infrastructure.messaging.events;

import java.util.UUID;

public record TeamScore(UUID teamId, String name, Integer score) {
}
