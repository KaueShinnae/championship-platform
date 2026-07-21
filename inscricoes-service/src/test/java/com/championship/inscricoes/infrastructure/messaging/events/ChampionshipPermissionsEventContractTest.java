package com.championship.inscricoes.infrastructure.messaging.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ChampionshipPermissionsEventContractTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void serializaPermissionsChangedComSchemaDocumentado() throws Exception {
        UUID championshipId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        ChampionshipPermissionsChangedPayload payload = new ChampionshipPermissionsChangedPayload(
                championshipId, ownerId, List.of(adminId));
        DomainEventEnvelope<ChampionshipPermissionsChangedPayload> envelope =
                DomainEventEnvelope.of(championshipId, ChampionshipPermissionsChangedPayload.TYPE, payload);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(envelope));

        assertThat(json.get("event_id").asText()).isEqualTo(envelope.eventId().toString());
        assertThat(json.get("occurred_at")).isNotNull();
        assertThat(json.get("aggregate_id").asText()).isEqualTo(championshipId.toString());
        assertThat(json.get("type").asText()).isEqualTo("championship.permissions.changed.v1");

        JsonNode payloadJson = json.get("payload");
        assertThat(payloadJson.get("championship_id").asText()).isEqualTo(championshipId.toString());
        assertThat(payloadJson.get("owner_id").asText()).isEqualTo(ownerId.toString());
        assertThat(payloadJson.get("admin_ids").get(0).asText()).isEqualTo(adminId.toString());

        DomainEventEnvelope<ChampionshipPermissionsChangedPayload> roundTrip = objectMapper.readValue(
                objectMapper.writeValueAsString(envelope),
                objectMapper.getTypeFactory().constructParametricType(
                        DomainEventEnvelope.class, ChampionshipPermissionsChangedPayload.class));
        assertThat(roundTrip.payload().ownerId()).isEqualTo(ownerId);
        assertThat(roundTrip.payload().adminIds()).containsExactly(adminId);
    }
}
