package com.championship.inscricoes.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampeonatoTest {

    @Test
    void criaCampeonatoAbertoPorPadrao() {
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026");

        assertThat(campeonato.getId()).isNotNull();
        assertThat(campeonato.getStatus()).isEqualTo(CampeonatoStatus.ABERTO);
        assertThat(campeonato.aceitaInscricoes()).isTrue();
    }

    @Test
    void rejeitaNomeMuitoLongo() {
        String nomeGigante = "a".repeat(101);
        assertThatThrownBy(() -> Campeonato.criar(nomeGigante))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
