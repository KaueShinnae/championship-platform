package com.championship.partidas.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartidaTest {

    private final UUID campeonatoId = UUID.randomUUID();
    private final UUID homeTeamId = UUID.randomUUID();
    private final UUID awayTeamId = UUID.randomUUID();

    private Partida agendada() {
        return Partida.agendar(campeonatoId, null, homeTeamId, "Timaço FC", awayTeamId, "Rival FC",
                Instant.parse("2026-07-20T19:30:00Z"));
    }

    @Test
    void agendaPartidaComHorarioMarcado() {
        Partida partida = agendada();

        assertThat(partida.getId()).isNotNull();
        assertThat(partida.getStatus()).isEqualTo(PartidaStatus.AGENDADA);
        assertThat(partida.getScheduledAt()).isEqualTo(Instant.parse("2026-07-20T19:30:00Z"));
        assertThat(partida.getStartedAt()).isNull();
        assertThat(partida.getHomeScore()).isNull();
    }

    @Test
    void agendaSemHorarioUsaAgora() {
        Partida partida = Partida.agendar(campeonatoId, null, homeTeamId, "Timaço FC", awayTeamId, "Rival FC", null);

        assertThat(partida.getScheduledAt()).isNotNull();
    }

    @Test
    void rejeitaTimeJogandoContraSiMesmo() {
        assertThatThrownBy(() -> Partida.agendar(campeonatoId, null, homeTeamId, "Timaço FC", homeTeamId, "Timaço FC", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void iniciarColocaEmAndamento() {
        Partida partida = agendada();

        partida.iniciar();

        assertThat(partida.getStatus()).isEqualTo(PartidaStatus.EM_ANDAMENTO);
        assertThat(partida.getStartedAt()).isNotNull();
    }

    @Test
    void rejeitaIniciarDuasVezes() {
        Partida partida = agendada();
        partida.iniciar();

        assertThatThrownBy(partida::iniciar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejeitaResultadoComPartidaApenasAgendada() {
        Partida partida = agendada();

        assertThatThrownBy(() -> partida.registrarResultado(2, 1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void registraResultadoComPartidaEmAndamento() {
        Partida partida = agendada();
        partida.iniciar();

        partida.registrarResultado(2, 1);

        assertThat(partida.getStatus()).isEqualTo(PartidaStatus.FINALIZADA);
        assertThat(partida.getHomeScore()).isEqualTo(2);
        assertThat(partida.getAwayScore()).isEqualTo(1);
        assertThat(partida.getPlayedAt()).isNotNull();
    }

    @Test
    void rejeitaRegistrarResultadoDuasVezes() {
        Partida partida = agendada();
        partida.iniciar();
        partida.registrarResultado(2, 1);

        assertThatThrownBy(() -> partida.registrarResultado(0, 0))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejeitaPlacarNegativo() {
        Partida partida = agendada();
        partida.iniciar();

        assertThatThrownBy(() -> partida.registrarResultado(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
