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
    void agendaSemHorarioFicaADefinir() {
        // nunca inventar data: sem horario escolhido, scheduled_at fica null
        Partida partida = Partida.agendar(campeonatoId, null, homeTeamId, "Timaço FC", awayTeamId, "Rival FC", null);

        assertThat(partida.getScheduledAt()).isNull();
        assertThat(partida.getStatus()).isEqualTo(PartidaStatus.AGENDADA);
    }

    @Test
    void rejeitaTimeJogandoContraSiMesmo() {
        assertThatThrownBy(() -> Partida.agendar(campeonatoId, null, homeTeamId, "Timaço FC", homeTeamId, "Timaço FC", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reagendaPartidaAgendada() {
        Partida partida = agendada();

        partida.reagendar(Instant.parse("2026-07-22T20:00:00Z"));

        assertThat(partida.getScheduledAt()).isEqualTo(Instant.parse("2026-07-22T20:00:00Z"));
        assertThat(partida.getStatus()).isEqualTo(PartidaStatus.AGENDADA);
    }

    @Test
    void rejeitaReagendarPartidaJaIniciada() {
        Partida partida = agendada();
        partida.iniciar();

        assertThatThrownBy(() -> partida.reagendar(Instant.parse("2026-07-22T20:00:00Z")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejeitaReagendarSemHorario() {
        Partida partida = agendada();

        assertThatThrownBy(() -> partida.reagendar(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void agendaComLocalEReagendaMantemLocal() {
        Partida partida = Partida.agendar(campeonatoId, null, homeTeamId, "Timaço FC", awayTeamId, "Rival FC",
                Instant.parse("2026-07-20T19:30:00Z"), "Quadra 1");
        assertThat(partida.getLocal()).isEqualTo("Quadra 1");

        // reagendar sem informar local (overload de 1 arg) preserva o local atual
        partida.reagendar(Instant.parse("2026-07-22T20:00:00Z"));
        assertThat(partida.getLocal()).isEqualTo("Quadra 1");

        // reagendar informando novo local o troca; em branco vira "a definir" (null)
        partida.reagendar(Instant.parse("2026-07-22T20:00:00Z"), "Ginásio Central");
        assertThat(partida.getLocal()).isEqualTo("Ginásio Central");
        partida.reagendar(Instant.parse("2026-07-22T20:00:00Z"), "   ");
        assertThat(partida.getLocal()).isNull();
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
    void atualizaPlacarParcialComPartidaEmAndamento() {
        Partida partida = agendada();
        partida.iniciar();

        partida.atualizarPlacar(2, 2);

        assertThat(partida.getHomeScore()).isEqualTo(2);
        assertThat(partida.getAwayScore()).isEqualTo(2);
        assertThat(partida.getStatus()).isEqualTo(PartidaStatus.EM_ANDAMENTO);
    }

    @Test
    void placarParcialAceitaEmpateAteMesmoEmPlayoff() {
        // a exigencia de vencedor no mata-mata vale so no encerramento
        Partida playoff = Partida.dePlayoff(campeonatoId, 1, 0, homeTeamId, "Timaço FC", awayTeamId, "Rival FC");
        playoff.iniciar();

        playoff.atualizarPlacar(1, 1);

        assertThat(playoff.getStatus()).isEqualTo(PartidaStatus.EM_ANDAMENTO);
    }

    @Test
    void rejeitaPlacarParcialForaDeJogoOuNegativo() {
        Partida agendada = agendada();
        assertThatThrownBy(() -> agendada.atualizarPlacar(1, 0)).isInstanceOf(IllegalStateException.class);

        Partida emJogo = agendada();
        emJogo.iniciar();
        assertThatThrownBy(() -> emJogo.atualizarPlacar(-1, 0)).isInstanceOf(IllegalArgumentException.class);
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

    @Test
    void marcaResultadoComoWo() {
        Partida partida = agendada();
        partida.iniciar();

        partida.registrarResultado(1, 0, true, "adversário não compareceu");

        assertThat(partida.isWo()).isTrue();
        assertThat(partida.getWoMotivo()).isEqualTo("adversário não compareceu");
        assertThat(partida.getStatus()).isEqualTo(PartidaStatus.FINALIZADA);
    }

    @Test
    void corrigeResultadoJaRegistrado() {
        Partida partida = agendada();
        partida.iniciar();
        partida.registrarResultado(3, 1);

        partida.corrigirResultado(1, 3);

        assertThat(partida.getHomeScore()).isEqualTo(1);
        assertThat(partida.getAwayScore()).isEqualTo(3);
        assertThat(partida.getStatus()).isEqualTo(PartidaStatus.FINALIZADA);
    }

    @Test
    void naoCorrigeResultadoAindaNaoRegistrado() {
        Partida partida = agendada();
        partida.iniciar();

        assertThatThrownBy(() -> partida.corrigirResultado(1, 0))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void substituiTimeDePartidaAgendada() {
        Partida partida = agendada();
        UUID novo = UUID.randomUUID();

        partida.substituirTime(true, novo, "Novo FC");

        assertThat(partida.getHomeTeamId()).isEqualTo(novo);
        assertThat(partida.getHomeTeamName()).isEqualTo("Novo FC");
    }
}
