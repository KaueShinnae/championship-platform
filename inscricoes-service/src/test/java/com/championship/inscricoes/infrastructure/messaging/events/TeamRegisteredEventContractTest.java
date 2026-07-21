package com.championship.inscricoes.infrastructure.messaging.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class TeamRegisteredEventContractTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void serializaTeamRegisteredComSchemaDocumentado() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID championshipId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        TeamRegisteredPayload payload = new TeamRegisteredPayload(
                teamId, "Timaço FC", championshipId, List.of(new PlayerRef(playerId, "Jogador 1")));
        DomainEventEnvelope<TeamRegisteredPayload> envelope = DomainEventEnvelope.of(teamId, TeamRegisteredPayload.TYPE, payload);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(envelope));

        assertThat(json.get("event_id").asText()).isEqualTo(envelope.eventId().toString());
        assertThat(json.get("occurred_at")).isNotNull();
        assertThat(json.get("aggregate_id").asText()).isEqualTo(teamId.toString());
        assertThat(json.get("type").asText()).isEqualTo("team.registered.v1");

        JsonNode payloadJson = json.get("payload");
        assertThat(payloadJson.get("team_id").asText()).isEqualTo(teamId.toString());
        assertThat(payloadJson.get("team_name").asText()).isEqualTo("Timaço FC");
        assertThat(payloadJson.get("championship_id").asText()).isEqualTo(championshipId.toString());
        assertThat(payloadJson.get("players").get(0).get("player_id").asText()).isEqualTo(playerId.toString());
        assertThat(payloadJson.get("players").get(0).get("name").asText()).isEqualTo("Jogador 1");
    }

    @Test
    void serializaEnrollmentConfirmedComSchemaDocumentado() throws Exception {
        UUID enrollmentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID championshipId = UUID.randomUUID();
        Instant confirmedAt = Instant.parse("2026-07-09T14:32:05Z");

        EnrollmentConfirmedPayload payload = new EnrollmentConfirmedPayload(
                enrollmentId, teamId, championshipId, null, confirmedAt);
        DomainEventEnvelope<EnrollmentConfirmedPayload> envelope =
                DomainEventEnvelope.of(enrollmentId, EnrollmentConfirmedPayload.TYPE, payload);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(envelope));

        assertThat(json.get("type").asText()).isEqualTo("enrollment.confirmed.v1");
        JsonNode payloadJson = json.get("payload");
        assertThat(payloadJson.get("enrollment_id").asText()).isEqualTo(enrollmentId.toString());
        assertThat(payloadJson.get("team_id").asText()).isEqualTo(teamId.toString());
        assertThat(payloadJson.get("championship_id").asText()).isEqualTo(championshipId.toString());
        assertThat(payloadJson.get("confirmed_at").asText()).contains("2026-07-09");

        DomainEventEnvelope<EnrollmentConfirmedPayload> roundTrip = objectMapper.readValue(
                objectMapper.writeValueAsString(envelope),
                objectMapper.getTypeFactory().constructParametricType(DomainEventEnvelope.class, EnrollmentConfirmedPayload.class));
        assertThat(roundTrip.payload().teamId()).isEqualTo(teamId);
    }
}
