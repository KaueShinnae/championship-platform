package com.championship.ranking.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model CQRS (SPEC.md §3): populado só por eventos consumidos do
 * Kafka, nunca por escrita direta via API. Uma linha por (group_id, team_id).
 */
@Entity
@Table(name = "group_standing", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "team_id"}))
public class GroupStanding {

    @Id
    private UUID id;

    @Column(name = "campeonato_id", nullable = false)
    private UUID campeonatoId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Column(nullable = false)
    private int points = 0;

    @Column(nullable = false)
    private int wins = 0;

    @Column(nullable = false)
    private int draws = 0;

    @Column(nullable = false)
    private int losses = 0;

    @Column(name = "goals_for", nullable = false)
    private int goalsFor = 0;

    @Column(name = "goals_against", nullable = false)
    private int goalsAgainst = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GroupStanding() {
    }

    private GroupStanding(UUID id, UUID campeonatoId, UUID groupId, UUID teamId, String teamName, Instant updatedAt) {
        this.id = id;
        this.campeonatoId = campeonatoId;
        this.groupId = groupId;
        this.teamId = teamId;
        this.teamName = teamName;
        this.updatedAt = updatedAt;
    }

    public static GroupStanding inicial(UUID campeonatoId, UUID groupId, UUID teamId, String teamName) {
        return new GroupStanding(UUID.randomUUID(), campeonatoId, groupId, teamId, teamName, Instant.now());
    }

    /**
     * Aplica o resultado de uma partida do ponto de vista deste time.
     * Vitória = 3 pontos, empate = 1 ponto, derrota = 0 (docs/events/match.finished.v1.md).
     */
    public void aplicarResultado(int golsPro, int golsContra) {
        if (golsPro < 0 || golsContra < 0) {
            throw new IllegalArgumentException("placar nao pode ser negativo");
        }
        if (golsPro > golsContra) {
            this.wins++;
            this.points += 3;
        } else if (golsPro == golsContra) {
            this.draws++;
            this.points += 1;
        } else {
            this.losses++;
        }
        this.goalsFor += golsPro;
        this.goalsAgainst += golsContra;
        this.updatedAt = Instant.now();
    }

    public void atualizarNomeTime(String teamName) {
        this.teamName = teamName;
    }

    public int getGoalDifference() {
        return goalsFor - goalsAgainst;
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

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public int getPoints() {
        return points;
    }

    public int getWins() {
        return wins;
    }

    public int getDraws() {
        return draws;
    }

    public int getLosses() {
        return losses;
    }

    public int getGoalsFor() {
        return goalsFor;
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
