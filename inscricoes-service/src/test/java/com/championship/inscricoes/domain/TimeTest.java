package com.championship.inscricoes.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeTest {

    @Test
    void editaNomeEElencoSemRecriar() {
        Time time = Time.criar("Timaço FC", List.of("Jogador 1"));
        java.util.UUID idOriginal = time.getId();

        time.editar("Timação FC", List.of("Novo 1", "Novo 2", "Novo 3"));

        assertThat(time.getId()).isEqualTo(idOriginal); // mesma entidade
        assertThat(time.getNome()).isEqualTo("Timação FC");
        assertThat(time.getJogadores()).extracting(Jogador::getNome)
                .containsExactly("Novo 1", "Novo 2", "Novo 3");
    }

    @Test
    void editarExigePeloMenosUmJogador() {
        Time time = Time.criar("Timaço FC", List.of("Jogador 1"));

        assertThatThrownBy(() -> time.editar("Timaço FC", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void criaTimeComJogadores() {
        Time time = Time.criar("Timaço FC", List.of("Jogador 1", "Jogador 2"));

        assertThat(time.getId()).isNotNull();
        assertThat(time.getNome()).isEqualTo("Timaço FC");
        assertThat(time.getJogadores()).hasSize(2);
        assertThat(time.getJogadores()).extracting(Jogador::getNome)
                .containsExactly("Jogador 1", "Jogador 2");
    }

    @Test
    void rejeitaNomeVazio() {
        assertThatThrownBy(() -> Time.criar(" ", List.of("Jogador 1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaTimeSemJogadores() {
        assertThatThrownBy(() -> Time.criar("Timaço FC", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
