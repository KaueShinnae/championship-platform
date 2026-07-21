package com.championship.partidas.infrastructure.messaging.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class MatchEventsContractTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void serializaMatchScheduledComSchemaDocumentado() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID championshipId = UUID.randomUUID();
        UUID homeTeamId = UUID.randomUUID();
        UUID awayTeamId = UUID.randomUUID();

        MatchScheduledPayload payload = new MatchScheduledPayload(
                matchId, championshipId, null,
                new TeamRef(homeTeamId, "Timaço FC"),
                new TeamRef(awayTeamId, "Rival FC"),
                Instant.parse("2026-07-16T10:00:00Z"),
                "Quadra 2");
        DomainEventEnvelope<MatchScheduledPayload> envelope = DomainEventEnvelope.of(matchId, MatchScheduledPayload.TYPE, payload);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(envelope));

        assertThat(json.get("type").asText()).isEqualTo("match.scheduled.v1");
        JsonNode payloadJson = json.get("payload");
        assertThat(payloadJson.get("match_id").asText()).isEqualTo(matchId.toString());
        assertThat(payloadJson.get("championship_id").asText()).isEqualTo(championshipId.toString());
        assertThat(payloadJson.get("home_team").get("team_id").asText()).isEqualTo(homeTeamId.toString());
        assertThat(payloadJson.get("home_team").get("name").asText()).isEqualTo("Timaço FC");
        assertThat(payloadJson.get("away_team").get("team_id").asText()).isEqualTo(awayTeamId.toString());
        assertThat(payloadJson.get("local").asText()).isEqualTo("Quadra 2");
    }

    @Test
    void serializaMatchStartedComSchemaDocumentado() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID championshipId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        MatchStartedPayload payload = new MatchStartedPayload(
                matchId, championshipId, groupId, Instant.parse("2026-07-16T12:00:00Z"));
        DomainEventEnvelope<MatchStartedPayload> envelope =
                DomainEventEnvelope.of(matchId, MatchStartedPayload.TYPE, payload);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(envelope));

        assertThat(json.get("type").asText()).isEqualTo("match.started.v1");
        JsonNode payloadJson = json.get("payload");
        assertThat(payloadJson.get("match_id").asText()).isEqualTo(matchId.toString());
        assertThat(payloadJson.get("championship_id").asText()).isEqualTo(championshipId.toString());
        assertThat(payloadJson.get("group_id").asText()).isEqualTo(groupId.toString());
        assertThat(payloadJson.get("started_at").asText()).contains("2026-07-16");
    }

    @Test
    void serializaChampionshipCompletedComSchemaDocumentado() throws Exception {
        UUID championshipId = UUID.randomUUID();
        UUID championId = UUID.randomUUID();

        ChampionshipCompletedPayload payload = new ChampionshipCompletedPayload(
                championshipId, new TeamRef(championId, "Timaço FC"), Instant.parse("2026-07-16T12:00:00Z"));
        DomainEventEnvelope<ChampionshipCompletedPayload> envelope =
                DomainEventEnvelope.of(championshipId, ChampionshipCompletedPayload.TYPE, payload);

        String json = objectMapper.writeValueAsString(envelope);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("type").asText()).isEqualTo("championship.completed.v1");
        assertThat(node.get("aggregate_id").asText()).isEqualTo(championshipId.toString());
        JsonNode payloadJson = node.get("payload");
        assertThat(payloadJson.get("championship_id").asText()).isEqualTo(championshipId.toString());
        assertThat(payloadJson.get("champion").get("team_id").asText()).isEqualTo(championId.toString());
        assertThat(payloadJson.get("champion").get("name").asText()).isEqualTo("Timaço FC");
        assertThat(payloadJson.get("completed_at").asText()).contains("2026-07-16");

        DomainEventEnvelope<ChampionshipCompletedPayload> roundTrip = objectMapper.readValue(
                json, objectMapper.getTypeFactory().constructParametricType(
                        DomainEventEnvelope.class, ChampionshipCompletedPayload.class));
        assertThat(roundTrip.payload().champion().name()).isEqualTo("Timaço FC");
    }

    @Test
    void serializaMatchFinishedComSchemaDocumentado() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID championshipId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID homeTeamId = UUID.randomUUID();
        UUID awayTeamId = UUID.randomUUID();

        MatchFinishedPayload payload = new MatchFinishedPayload(
                matchId, championshipId, groupId,
                new TeamScore(homeTeamId, "Timaço FC", 2),
                new TeamScore(awayTeamId, "Rival FC", 1),
                false,
                Instant.parse("2026-07-16T12:00:00Z"));
        DomainEventEnvelope<MatchFinishedPayload> envelope = DomainEventEnvelope.of(matchId, MatchFinishedPayload.TYPE, payload);

        String json = objectMapper.writeValueAsString(envelope);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("type").asText()).isEqualTo("match.finished.v1");
        JsonNode payloadJson = node.get("payload");
        assertThat(payloadJson.get("group_id").asText()).isEqualTo(groupId.toString());
        assertThat(payloadJson.get("home_team").get("score").asInt()).isEqualTo(2);
        assertThat(payloadJson.get("away_team").get("score").asInt()).isEqualTo(1);

        DomainEventEnvelope<MatchFinishedPayload> roundTrip = objectMapper.readValue(
                json, objectMapper.getTypeFactory().constructParametricType(DomainEventEnvelope.class, MatchFinishedPayload.class));
        assertThat(roundTrip.payload().homeTeam().score()).isEqualTo(2);
        assertThat(roundTrip.payload().groupId()).isEqualTo(groupId);
    }

    @Test
    void serializaMatchResultCorrectedComSchemaDocumentado() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID championshipId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID homeTeamId = UUID.randomUUID();
        UUID awayTeamId = UUID.randomUUID();

        MatchResultCorrectedPayload payload = new MatchResultCorrectedPayload(
                matchId, championshipId, groupId,
                new TeamScore(homeTeamId, "Timaço FC", 3),
                new TeamScore(awayTeamId, "Rival FC", 1),
                new TeamScore(homeTeamId, "Timaço FC", 1),
                new TeamScore(awayTeamId, "Rival FC", 3),
                false,
                Instant.parse("2026-07-17T12:00:00Z"));
        DomainEventEnvelope<MatchResultCorrectedPayload> envelope =
                DomainEventEnvelope.of(matchId, MatchResultCorrectedPayload.TYPE, payload);

        String json = objectMapper.writeValueAsString(envelope);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("type").asText()).isEqualTo("match.result.corrected.v1");
        JsonNode payloadJson = node.get("payload");
        assertThat(payloadJson.get("previous_home").get("score").asInt()).isEqualTo(3);
        assertThat(payloadJson.get("corrected_home").get("score").asInt()).isEqualTo(1);
        assertThat(payloadJson.get("corrected_away").get("score").asInt()).isEqualTo(3);

        DomainEventEnvelope<MatchResultCorrectedPayload> roundTrip = objectMapper.readValue(
                json, objectMapper.getTypeFactory().constructParametricType(
                        DomainEventEnvelope.class, MatchResultCorrectedPayload.class));
        assertThat(roundTrip.payload().previousHome().score()).isEqualTo(3);
        assertThat(roundTrip.payload().correctedHome().score()).isEqualTo(1);
    }
}
