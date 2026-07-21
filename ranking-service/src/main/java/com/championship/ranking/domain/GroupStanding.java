package com.championship.ranking.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

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

    public void aplicarResultado(int pontosPro, int pontosContra) {
        aplicarResultado(pontosPro, pontosContra, false);
    }

    public void aplicarResultado(int pontosPro, int pontosContra, boolean wo) {
        if (pontosPro < 0 || pontosContra < 0) {
            throw new IllegalArgumentException("placar nao pode ser negativo");
        }
        if (pontosPro > pontosContra) {
            this.wins++;
            this.points += 3;
        } else if (pontosPro == pontosContra) {
            this.draws++;
            this.points += 1;
        } else {
            this.losses++;
        }
        if (!wo) {
            this.goalsFor += pontosPro;
            this.goalsAgainst += pontosContra;
        }
        this.updatedAt = Instant.now();
    }

    public void reverterResultado(int pontosPro, int pontosContra) {
        reverterResultado(pontosPro, pontosContra, false);
    }

    public void reverterResultado(int pontosPro, int pontosContra, boolean wo) {
        if (pontosPro > pontosContra) {
            this.wins--;
            this.points -= 3;
        } else if (pontosPro == pontosContra) {
            this.draws--;
            this.points -= 1;
        } else {
            this.losses--;
        }
        if (!wo) {
            this.goalsFor -= pontosPro;
            this.goalsAgainst -= pontosContra;
        }
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
