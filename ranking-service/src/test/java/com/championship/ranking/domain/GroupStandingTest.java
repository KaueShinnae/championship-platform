package com.championship.ranking.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupStandingTest {

    private GroupStanding novoStanding() {
        return GroupStanding.inicial(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Timaço FC");
    }

    @Test
    void vitoriaVale3Pontos() {
        GroupStanding standing = novoStanding();

        standing.aplicarResultado(2, 1);

        assertThat(standing.getPoints()).isEqualTo(3);
        assertThat(standing.getWins()).isEqualTo(1);
        assertThat(standing.getDraws()).isZero();
        assertThat(standing.getLosses()).isZero();
    }

    @Test
    void empateVale1Ponto() {
        GroupStanding standing = novoStanding();

        standing.aplicarResultado(1, 1);

        assertThat(standing.getPoints()).isEqualTo(1);
        assertThat(standing.getDraws()).isEqualTo(1);
    }

    @Test
    void derrotaVale0Pontos() {
        GroupStanding standing = novoStanding();

        standing.aplicarResultado(0, 3);

        assertThat(standing.getPoints()).isZero();
        assertThat(standing.getLosses()).isEqualTo(1);
    }

    @Test
    void acumulaSaldoDeGols() {
        GroupStanding standing = novoStanding();

        standing.aplicarResultado(3, 1); // vitoria, saldo +2
        standing.aplicarResultado(0, 2); // derrota, saldo -2
        standing.aplicarResultado(1, 1); // empate, saldo 0

        assertThat(standing.getPoints()).isEqualTo(4);
        assertThat(standing.getGoalsFor()).isEqualTo(4);
        assertThat(standing.getGoalsAgainst()).isEqualTo(4);
        assertThat(standing.getGoalDifference()).isZero();
    }

    @Test
    void rejeitaPlacarNegativo() {
        assertThatThrownBy(() -> novoStanding().aplicarResultado(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void woContaVitoriaMasEhNeutroNoPlacar() {
        GroupStanding standing = novoStanding();

        standing.aplicarResultado(1, 0, true); // W.O. a favor

        assertThat(standing.getPoints()).isEqualTo(3);
        assertThat(standing.getWins()).isEqualTo(1);
        assertThat(standing.getGoalsFor()).isZero();     // neutro no placar
        assertThat(standing.getGoalsAgainst()).isZero();
        assertThat(standing.getGoalDifference()).isZero();
    }

    @Test
    void correcaoRevertteOAntigoEAplicaONovo() {
        GroupStanding standing = novoStanding();
        standing.aplicarResultado(3, 1); // vitória registrada por engano (era derrota)

        // correção: reverte 3x1 e aplica 1x3
        standing.reverterResultado(3, 1);
        standing.aplicarResultado(1, 3);

        assertThat(standing.getPoints()).isZero();
        assertThat(standing.getWins()).isZero();
        assertThat(standing.getLosses()).isEqualTo(1);
        assertThat(standing.getGoalsFor()).isEqualTo(1);
        assertThat(standing.getGoalsAgainst()).isEqualTo(3);
        assertThat(standing.getGoalDifference()).isEqualTo(-2);
    }
}
