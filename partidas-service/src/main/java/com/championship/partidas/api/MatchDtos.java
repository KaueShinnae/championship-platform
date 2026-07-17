package com.championship.partidas.api;

import com.championship.partidas.domain.FormatoTorneio;
import com.championship.partidas.domain.Partida;
import com.championship.partidas.domain.PartidaStage;
import com.championship.partidas.domain.PartidaStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
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

    public record ReagendarPartidaRequest(
            @NotNull Instant scheduledAt
    ) {
    }

    public record TimeSorteio(
            @NotNull UUID teamId,
            @NotBlank @Size(max = 100) String name
    ) {
    }

    public record GerarConfrontosRequest(
            @NotNull UUID championshipId,
            @NotNull FormatoTorneio formato,
            @NotEmpty List<@Valid TimeSorteio> teams
    ) {
    }

    public record TeamView(UUID teamId, String name, Integer score) {
    }

    /** Slot ocupado do bracket (inclui byes: time que entrou numa rodada sem partida ainda). */
    public record ChaveSlotResponse(int round, int slot, UUID teamId, String teamName) {
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
            Instant playedAt,
            PartidaStage stage,
            Integer round,
            Integer bracketPos
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
                    partida.getPlayedAt(),
                    partida.getStage(),
                    partida.getRound(),
                    partida.getBracketPos());
        }
    }

    private MatchDtos() {
    }
}
