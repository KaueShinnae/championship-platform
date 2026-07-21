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
            Instant scheduledAt,
            @Size(max = 120) String local
    ) {
    }

    public record RegistrarResultadoRequest(
            @NotNull @Min(0) Integer homeScore,
            @NotNull @Min(0) Integer awayScore,
            Boolean wo,            // opcional: resultado por decisão administrativa (W.O.)
            @Size(max = 200) String woMotivo
    ) {
    }

    public record CorrigirResultadoRequest(
            @NotNull @Min(0) Integer homeScore,
            @NotNull @Min(0) Integer awayScore
    ) {
    }

    public record ReagendarPartidaRequest(
            @NotNull Instant scheduledAt,
            @Size(max = 120) String local
    ) {
    }

    public record ReagendarEmLoteRequest(
            @NotNull UUID championshipId,
            @NotNull Integer shiftMinutes
    ) {
    }

    public record ConflitoHorarioResponse(
            UUID partidaA, UUID partidaB, String tipo,
            UUID teamId, String teamName, String local, Instant scheduledAt
    ) {
    }

    public record GestaoLogResponse(
            UUID id, UUID actorId, String actorNome, String acao, String descricao, Instant createdAt
    ) {
    }

    public record DesistenciaRequest(
            @NotNull UUID championshipId,
            @NotNull UUID teamId
    ) {
    }

    public record StandingEntryResponse(
            UUID teamId, String teamName,
            int pontos, int vitorias, int empates, int derrotas,
            int pro, int contra, int saldo,
            String desempate
    ) {
    }

    public record GroupStandingsResponse(UUID groupId, List<StandingEntryResponse> standings) {
    }

    public record TimeSorteio(
            @NotNull UUID teamId,
            @NotBlank @Size(max = 100) String name
    ) {
    }

    public record GerarConfrontosRequest(
            @NotNull UUID championshipId,
            @NotNull FormatoTorneio formato,
            @NotEmpty List<@Valid TimeSorteio> teams,
            Boolean disputaTerceiro // disputa de 3º lugar no mata-mata
    ) {
    }

    public record TeamView(UUID teamId, String name, Integer score) {
    }

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
            String local,
            Instant startedAt,
            Instant playedAt,
            PartidaStage stage,
            Integer round,
            Integer bracketPos,
            boolean wo,
            String woMotivo,
            boolean terceiroLugar
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
                    partida.getLocal(),
                    partida.getStartedAt(),
                    partida.getPlayedAt(),
                    partida.getStage(),
                    partida.getRound(),
                    partida.getBracketPos(),
                    partida.isWo(),
                    partida.getWoMotivo(),
                    partida.isTerceiroLugar());
        }
    }

    private MatchDtos() {
    }
}
