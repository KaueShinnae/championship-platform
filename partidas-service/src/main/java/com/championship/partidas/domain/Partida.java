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

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "local", length = 120)
    private String local;

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

    @Column(nullable = false)
    private boolean wo = false;

    @Column(name = "wo_motivo", length = 200)
    private String woMotivo;

    @Column(name = "terceiro_lugar", nullable = false)
    private boolean terceiroLugar = false;

    protected Partida() {
    }

    private Partida(UUID id, UUID campeonatoId, UUID groupId,
                     UUID homeTeamId, String homeTeamName,
                     UUID awayTeamId, String awayTeamName,
                     Instant scheduledAt, String local,
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
        this.local = normalizarLocal(local);
        this.createdAt = Instant.now();
        this.stage = stage;
        this.round = round;
        this.bracketPos = bracketPos;
    }

    public static Partida agendar(UUID campeonatoId, UUID groupId,
                                   UUID homeTeamId, String homeTeamName,
                                   UUID awayTeamId, String awayTeamName,
                                   Instant scheduledAt) {
        return agendar(campeonatoId, groupId, homeTeamId, homeTeamName, awayTeamId, awayTeamName, scheduledAt, null);
    }

    public static Partida agendar(UUID campeonatoId, UUID groupId,
                                   UUID homeTeamId, String homeTeamName,
                                   UUID awayTeamId, String awayTeamName,
                                   Instant scheduledAt, String local) {
        validarTimes(homeTeamId, homeTeamName, awayTeamId, awayTeamName);
        // scheduledAt null = horario "a definir" (nunca inventar data)
        return new Partida(UUID.randomUUID(), campeonatoId, groupId,
                homeTeamId, homeTeamName, awayTeamId, awayTeamName,
                scheduledAt, local,
                groupId != null ? PartidaStage.GRUPOS : null, null, null);
    }

    public static Partida deGrupo(UUID campeonatoId, UUID groupId, int round,
                                   UUID homeTeamId, String homeTeamName,
                                   UUID awayTeamId, String awayTeamName) {
        validarTimes(homeTeamId, homeTeamName, awayTeamId, awayTeamName);
        return new Partida(UUID.randomUUID(), campeonatoId, groupId,
                homeTeamId, homeTeamName, awayTeamId, awayTeamName,
                null, null, PartidaStage.GRUPOS, round, null);
    }

    public static Partida dePlayoff(UUID campeonatoId, int round, int bracketPos,
                                     UUID homeTeamId, String homeTeamName,
                                     UUID awayTeamId, String awayTeamName) {
        validarTimes(homeTeamId, homeTeamName, awayTeamId, awayTeamName);
        return new Partida(UUID.randomUUID(), campeonatoId, null,
                homeTeamId, homeTeamName, awayTeamId, awayTeamName,
                null, null, PartidaStage.PLAYOFF, round, bracketPos);
    }

    public static Partida deTerceiroLugar(UUID campeonatoId, int round,
                                           UUID homeTeamId, String homeTeamName,
                                           UUID awayTeamId, String awayTeamName) {
        validarTimes(homeTeamId, homeTeamName, awayTeamId, awayTeamName);
        Partida partida = new Partida(UUID.randomUUID(), campeonatoId, null,
                homeTeamId, homeTeamName, awayTeamId, awayTeamName,
                null, null, PartidaStage.PLAYOFF, round, 1);
        partida.terceiroLugar = true;
        return partida;
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

    public void reagendar(Instant novoHorario) {
        reagendar(novoHorario, this.local);
    }

    public void reagendar(Instant novoHorario, String local) {
        if (this.status != PartidaStatus.AGENDADA) {
            throw new IllegalStateException("so partida agendada pode ser remarcada: " + id + " esta " + status);
        }
        if (novoHorario == null) {
            throw new IllegalArgumentException("novo horario e obrigatorio");
        }
        this.scheduledAt = novoHorario;
        this.local = normalizarLocal(local);
    }

    public void iniciar() {
        if (this.status != PartidaStatus.AGENDADA) {
            throw new IllegalStateException("so partida agendada pode ser iniciada: " + id + " esta " + status);
        }
        this.status = PartidaStatus.EM_ANDAMENTO;
        this.startedAt = Instant.now();
    }

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
        registrarResultado(homeScore, awayScore, false, null);
    }

    public void registrarResultado(int homeScore, int awayScore, boolean wo, String motivo) {
        if (this.status != PartidaStatus.EM_ANDAMENTO) {
            throw new IllegalStateException(
                    "resultado so pode ser registrado com a partida em andamento: " + id + " esta " + status);
        }
        validarPlacar(homeScore, awayScore);
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.wo = wo;
        this.woMotivo = wo ? motivo : null;
        this.status = PartidaStatus.FINALIZADA;
        this.playedAt = Instant.now();
    }

    public void corrigirResultado(int homeScore, int awayScore) {
        if (this.status != PartidaStatus.FINALIZADA) {
            throw new IllegalStateException(
                    "so um resultado ja registrado pode ser corrigido: " + id + " esta " + status);
        }
        validarPlacar(homeScore, awayScore);
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }

    private void validarPlacar(int homeScore, int awayScore) {
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("placar nao pode ser negativo");
        }
        if (stage == PartidaStage.PLAYOFF && homeScore == awayScore) {
            throw new IllegalArgumentException(
                    "partida eliminatoria precisa de um vencedor — registre o placar final ja incluindo a decisao (prorrogacao/penaltis)");
        }
    }

    public void substituirTime(boolean casa, UUID teamId, String teamName) {
        if (this.status != PartidaStatus.AGENDADA) {
            throw new IllegalStateException("so uma partida agendada pode ter os times substituidos: " + id);
        }
        if (casa) {
            this.homeTeamId = teamId;
            this.homeTeamName = teamName;
        } else {
            this.awayTeamId = teamId;
            this.awayTeamName = teamName;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizarLocal(String local) {
        return isBlank(local) ? null : local.trim();
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

    public String getLocal() {
        return local;
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

    public boolean isWo() {
        return wo;
    }

    public String getWoMotivo() {
        return woMotivo;
    }

    public UUID vencedorId() {
        return homeScore > awayScore ? homeTeamId : awayTeamId;
    }

    public String vencedorNome() {
        return homeScore > awayScore ? homeTeamName : awayTeamName;
    }

    public UUID perdedorId() {
        return homeScore > awayScore ? awayTeamId : homeTeamId;
    }

    public String perdedorNome() {
        return homeScore > awayScore ? awayTeamName : homeTeamName;
    }

    public boolean isTerceiroLugar() {
        return terceiroLugar;
    }
}
