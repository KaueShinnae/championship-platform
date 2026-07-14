package com.championship.partidas.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChaveamentoTest {

    @Test
    void numeroDeGruposSegueAsFaixasDaEspecificacao() {
        // 6-11 times -> 2 grupos; 12-23 -> 4; 24+ -> 8 (sempre >= 3 por grupo)
        assertThat(Chaveamento.numeroDeGrupos(6)).isEqualTo(2);
        assertThat(Chaveamento.numeroDeGrupos(11)).isEqualTo(2);
        assertThat(Chaveamento.numeroDeGrupos(12)).isEqualTo(4);
        assertThat(Chaveamento.numeroDeGrupos(23)).isEqualTo(4);
        assertThat(Chaveamento.numeroDeGrupos(24)).isEqualTo(8);
    }

    @Test
    void rejeitaGruposComMenosDe6Times() {
        assertThatThrownBy(() -> Chaveamento.numeroDeGrupos(5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void distribuiEmRodizioComDiferencaMaximaDe1() {
        List<List<Integer>> grupos = Chaveamento.distribuirEmGrupos(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), 2);

        assertThat(grupos).hasSize(2);
        assertThat(grupos.get(0)).hasSize(6);
        assertThat(grupos.get(1)).hasSize(5);
        // rodizio: 1º item vai pro grupo A, 2º pro B, 3º pro A...
        assertThat(grupos.get(0)).containsExactly(1, 3, 5, 7, 9, 11);
    }

    @Test
    void todosContraTodosGeraNVezesNMenos1Sobre2Confrontos() {
        assertThat(Chaveamento.todosContraTodos(4)).hasSize(6);
        assertThat(Chaveamento.todosContraTodos(5)).hasSize(10);
    }

    @Test
    void bracketUsaProximaPotenciaDe2() {
        assertThat(Chaveamento.proximaPotenciaDe2(2)).isEqualTo(2);
        assertThat(Chaveamento.proximaPotenciaDe2(5)).isEqualTo(8);
        assertThat(Chaveamento.proximaPotenciaDe2(8)).isEqualTo(8);
        assertThat(Chaveamento.rodadas(2)).isEqualTo(1);
        assertThat(Chaveamento.rodadas(8)).isEqualTo(3);
        assertThat(Chaveamento.rodadas(16)).isEqualTo(4);
    }

    @Test
    void byesAlternamEntreAsMetadesDoBracket() {
        // bracket de 8 (4 slots na 2ª rodada), 3 byes: 0 (cima), 3 (baixo), 1 (cima)
        assertThat(Chaveamento.slotsDeBye(8, 3)).containsExactly(0, 3, 1);
        assertThat(Chaveamento.slotsDeBye(16, 2)).containsExactly(0, 7);
    }

    @Test
    void partidaEliminatoriaExigeVencedor() {
        Partida playoff = Partida.dePlayoff(java.util.UUID.randomUUID(), 1, 0,
                java.util.UUID.randomUUID(), "Timaço FC", java.util.UUID.randomUUID(), "Rival FC");
        playoff.iniciar();

        assertThatThrownBy(() -> playoff.registrarResultado(1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vencedor");

        playoff.registrarResultado(2, 1);
        assertThat(playoff.vencedorNome()).isEqualTo("Timaço FC");
    }
}
