package com.championship.ranking.infrastructure.messaging.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class RankingEventsContractTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void desserializaMatchFinishedExatamenteComoDocumentado() throws Exception {
        // JSON literal copiado de docs/events/match.finished.v1.md — se o
        // contrato do produtor mudar, este teste quebra antes da producao.
        String json = """
                {
                  "event_id": "0b6f4c9e-6a1d-4e2a-9b1f-2f4d8a7c3e51",
                  "occurred_at": "2026-07-16T12:00:00Z",
                  "aggregate_id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                  "type": "match.finished.v1",
                  "payload": {
                    "match_id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                    "championship_id": "550e8400-e29b-41d4-a716-446655440000",
                    "group_id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                    "home_team": { "team_id": "6ba7b811-9dad-11d1-80b4-00c04fd430c8", "name": "Timaço FC", "score": 2 },
                    "away_team": { "team_id": "6ba7b812-9dad-11d1-80b4-00c04fd430c8", "name": "Rival FC", "score": 1 },
                    "played_at": "2026-07-16T12:00:00Z"
                  }
                }
                """;

        DomainEventEnvelope<MatchFinishedPayload> envelope = objectMapper.readValue(
                json, objectMapper.getTypeFactory().constructParametricType(DomainEventEnvelope.class, MatchFinishedPayload.class));

        assertThat(envelope.eventId()).isEqualTo(UUID.fromString("0b6f4c9e-6a1d-4e2a-9b1f-2f4d8a7c3e51"));
        assertThat(envelope.type()).isEqualTo("match.finished.v1");
        assertThat(envelope.payload().groupId()).isEqualTo(UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"));
        assertThat(envelope.payload().homeTeam().name()).isEqualTo("Timaço FC");
        assertThat(envelope.payload().homeTeam().score()).isEqualTo(2);
        assertThat(envelope.payload().awayTeam().score()).isEqualTo(1);
        assertThat(envelope.payload().playedAt()).isEqualTo(Instant.parse("2026-07-16T12:00:00Z"));
    }

    @Test
    void serializaRankingUpdatedComSchemaDocumentado() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID championshipId = UUID.randomUUID();

        RankingUpdatedPayload payload = new RankingUpdatedPayload(
                groupId, championshipId, Instant.parse("2026-07-16T12:00:05Z"));
        DomainEventEnvelope<RankingUpdatedPayload> envelope =
                DomainEventEnvelope.of(groupId, RankingUpdatedPayload.TYPE, payload);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(envelope));

        assertThat(json.get("event_id")).isNotNull();
        assertThat(json.get("aggregate_id").asText()).isEqualTo(groupId.toString());
        assertThat(json.get("type").asText()).isEqualTo("ranking.updated.v1");
        JsonNode payloadJson = json.get("payload");
        assertThat(payloadJson.get("group_id").asText()).isEqualTo(groupId.toString());
        assertThat(payloadJson.get("championship_id").asText()).isEqualTo(championshipId.toString());
        assertThat(payloadJson.get("updated_at").asText()).contains("2026-07-16");
    }
}
