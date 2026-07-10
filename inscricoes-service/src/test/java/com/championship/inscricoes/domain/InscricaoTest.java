package com.championship.inscricoes.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InscricaoTest {

    @Test
    void iniciaPendenteEConfirmaUmaVez() {
        Time time = Time.criar("Timaço FC", List.of("Jogador 1"));
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026");
        Inscricao inscricao = Inscricao.pendente(time, campeonato);

        assertThat(inscricao.getStatus()).isEqualTo(InscricaoStatus.PENDENTE);
        assertThat(inscricao.getConfirmedAt()).isNull();

        inscricao.confirmar();

        assertThat(inscricao.getStatus()).isEqualTo(InscricaoStatus.CONFIRMADA);
        assertThat(inscricao.getConfirmedAt()).isNotNull();
    }

    @Test
    void confirmarDuasVezesEIdempotente() {
        Time time = Time.criar("Timaço FC", List.of("Jogador 1"));
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026");
        Inscricao inscricao = Inscricao.pendente(time, campeonato);

        inscricao.confirmar();
        var primeiraConfirmacao = inscricao.getConfirmedAt();
        inscricao.confirmar();

        assertThat(inscricao.getConfirmedAt()).isEqualTo(primeiraConfirmacao);
    }
}
