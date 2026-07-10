package com.championship.partidas.api;

import com.championship.partidas.domain.Partida;
import com.championship.partidas.domain.PartidaStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class MatchDtos {

    public record AgendarPartidaRequest(
            @NotNull UUID championshipId,
            UUID groupId,
            @NotNull UUID homeTeamId,
            @NotBlank @Size(max = 100) String homeTeamName,
            @NotNull UUID awayTeamId,
            @NotBlank @Size(max = 100) String awayTeamName,
            Instant scheduledAt
    ) {
    }

    public record RegistrarResultadoRequest(
            @NotNull @Min(0) Integer homeScore,
            @NotNull @Min(0) Integer awayScore
    ) {
    }

    public record TeamView(UUID teamId, String name, Integer score) {
    }

    public record PartidaResponse(
            UUID matchId,
            UUID championshipId,
            UUID groupId,
            TeamView homeTeam,
            TeamView awayTeam,
            PartidaStatus status,
            Instant scheduledAt,
            Instant startedAt,
            Instant playedAt
    ) {
        public static PartidaResponse from(Partida partida) {
            return new PartidaResponse(
                    partida.getId(),
                    partida.getCampeonatoId(),
                    partida.getGroupId(),
                    new TeamView(partida.getHomeTeamId(), partida.getHomeTeamName(), partida.getHomeScore()),
                    new TeamView(partida.getAwayTeamId(), partida.getAwayTeamName(), partida.getAwayScore()),
                    partida.getStatus(),
                    partida.getScheduledAt(),
                    partida.getStartedAt(),
                    partida.getPlayedAt());
        }
    }

    private MatchDtos() {
    }
}
