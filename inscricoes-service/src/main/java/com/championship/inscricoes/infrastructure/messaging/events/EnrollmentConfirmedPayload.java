package com.championship.inscricoes.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentConfirmedPayload(
        UUID enrollmentId,
        UUID teamId,
        UUID championshipId,
        UUID groupId,
        Instant confirmedAt
) {
    public static final String TYPE = "enrollment.confirmed.v1";
}
