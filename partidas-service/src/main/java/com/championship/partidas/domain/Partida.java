package com.championship.partidas.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partida")
public class Partida {

    @Id
    private UUID id;

    @Column(name = "campeonato_id", nullable = false)
    private UUID campeonatoId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "home_team_id", nullable = false)
    private UUID homeTeamId;

    @Column(name = "home_team_name", nullable = false, length = 100)
    private String homeTeamName;

    @Column(name = "away_team_id", nullable = false)
    private UUID awayTeamId;

    @Column(name = "away_team_name", nullable = false, length = 100)
    private String awayTeamName;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartidaStatus status;

    /** Horário marcado; null = "a definir" (organizador define depois). */
    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "played_at")
    private Instant playedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PartidaStage stage;

    @Column(name = "round")
    private Integer round;

    @Column(name = "bracket_pos")
    private Integer bracketPos;

    protected Partida() {
    }

    private Partida(UUID id, UUID campeonatoId, UUID groupId,
                     UUID homeTeamId, String homeTeamName,
                     UUID awayTeamId, String awayTeamName,
                     Instant scheduledAt,
                     PartidaStage stage, Integer round, Integer bracketPos) {
        this.id = id;
        this.campeonatoId = campeonatoId;
        this.groupId = groupId;
        this.homeTeamId = homeTeamId;
        this.homeTeamName = homeTeamName;
        this.awayTeamId = awayTeamId;
        this.awayTeamName = awayTeamName;
        this.status = PartidaStatus.AGENDADA;
        this.scheduledAt = scheduledAt;
        this.createdAt = Instant.now();
        this.stage = stage;
        this.round = round;
        this.bracketPos = bracketPos;
    }

    public static Partida agendar(UUID campeonatoId, UUID groupId,
                                   UUID homeTeamId, String homeTeamName,
                                   UUID awayTeamId, String awayTeamName,
                                   Instant scheduledAt) {
        validarTimes(homeTeamId, homeTeamName, awayTeamId, awayTeamName);
        // scheduledAt null = horario "a definir" (nunca inventar data)
        return new Partida(UUID.randomUUID(), campeonatoId, groupId,
                homeTeamId, homeTeamName, awayTeamId, awayTeamName,
                scheduledAt,
                groupId != null ? PartidaStage.GRUPOS : null, null, null);
    }

    /** Partida de mata-mata: pertence a uma rodada e a uma posição do bracket. */
    public static Partida dePlayoff(UUID campeonatoId, int round, int bracketPos,
                                     UUID homeTeamId, String homeTeamName,
                                     UUID awayTeamId, String awayTeamName) {
        validarTimes(homeTeamId, homeTeamName, awayTeamId, awayTeamName);
        return new Partida(UUID.randomUUID(), campeonatoId, null,
                homeTeamId, homeTeamName, awayTeamId, awayTeamName,
                null, PartidaStage.PLAYOFF, round, bracketPos);
    }

    private static void validarTimes(UUID homeTeamId, String homeTeamName,
                                      UUID awayTeamId, String awayTeamName) {
        if (homeTeamId == null || awayTeamId == null) {
            throw new IllegalArgumentException("home_team_id e away_team_id sao obrigatorios");
        }
        if (homeTeamId.equals(awayTeamId)) {
            throw new IllegalArgumentException("um time nao pode jogar contra si mesmo");
        }
        if (isBlank(homeTeamName) || isBlank(awayTeamName)) {
            throw new IllegalArgumentException("home_team_name e away_team_name sao obrigatorios");
        }
    }

    /** Remarca data/horário — só enquanto a partida não foi iniciada. */
    public void reagendar(Instant novoHorario) {
        if (this.status != PartidaStatus.AGENDADA) {
            throw new IllegalStateException("so partida agendada pode ser remarcada: " + id + " esta " + status);
        }
        if (novoHorario == null) {
            throw new IllegalArgumentException("novo horario e obrigatorio");
        }
        this.scheduledAt = novoHorario;
    }

    public void iniciar() {
        if (this.status != PartidaStatus.AGENDADA) {
            throw new IllegalStateException("so partida agendada pode ser iniciada: " + id + " esta " + status);
        }
        this.status = PartidaStatus.EM_ANDAMENTO;
        this.startedAt = Instant.now();
    }

    /**
     * Placar parcial durante a partida — ferramenta de contagem do organizador
     * (pontos genéricos, vale para qualquer esporte). Empate é permitido aqui;
     * a exigência de vencedor no mata-mata vale só no encerramento.
     */
    public void atualizarPlacar(int homeScore, int awayScore) {
        if (this.status != PartidaStatus.EM_ANDAMENTO) {
            throw new IllegalStateException(
                    "placar parcial so pode ser atualizado com a partida em andamento: " + id + " esta " + status);
        }
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("placar nao pode ser negativo");
        }
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }

    public void registrarResultado(int homeScore, int awayScore) {
        if (this.status != PartidaStatus.EM_ANDAMENTO) {
            throw new IllegalStateException(
                    "resultado so pode ser registrado com a partida em andamento: " + id + " esta " + status);
        }
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("placar nao pode ser negativo");
        }
        if (stage == PartidaStage.PLAYOFF && homeScore == awayScore) {
            throw new IllegalArgumentException(
                    "partida eliminatoria precisa de um vencedor — registre o placar final ja incluindo a decisao (prorrogacao/penaltis)");
        }
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = PartidaStatus.FINALIZADA;
        this.playedAt = Instant.now();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCampeonatoId() {
        return campeonatoId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getHomeTeamId() {
        return homeTeamId;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public UUID getAwayTeamId() {
        return awayTeamId;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public PartidaStatus getStatus() {
        return status;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public PartidaStage getStage() {
        return stage;
    }

    public Integer getRound() {
        return round;
    }

    public Integer getBracketPos() {
        return bracketPos;
    }

    /** Id do vencedor — só para partidas finalizadas sem empate (mata-mata). */
    public UUID vencedorId() {
        return homeScore > awayScore ? homeTeamId : awayTeamId;
    }

    public String vencedorNome() {
        return homeScore > awayScore ? homeTeamName : awayTeamName;
    }
}
