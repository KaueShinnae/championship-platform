package com.championship.inscricoes.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload de enrollment.confirmed.v1 — ver docs/events/enrollment.confirmed.v1.md.
 */
public record EnrollmentConfirmedPayload(
        UUID enrollmentId,
        UUID teamId,
        UUID championshipId,
        UUID groupId,
        Instant confirmedAt
) {
    public static final String TYPE = "enrollment.confirmed.v1";
}
