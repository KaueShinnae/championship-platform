package com.championship.partidas.application;

import com.championship.partidas.application.StandingsService.TeamStanding;
import com.championship.partidas.domain.Partida;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StandingsServiceTest {

    private final UUID grupo = UUID.randomUUID();
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID c = UUID.randomUUID();
    private final UUID d = UUID.randomUUID();

    private Partida agendada(UUID casa, String nomeCasa, UUID fora, String nomeFora) {
        return Partida.agendar(UUID.randomUUID(), grupo, casa, nomeCasa, fora, nomeFora, null);
    }

    private Partida jogada(UUID casa, String nomeCasa, UUID fora, String nomeFora, int hs, int as) {
        Partida p = agendada(casa, nomeCasa, fora, nomeFora);
        p.iniciar();
        p.registrarResultado(hs, as);
        return p;
    }

    private Partida wo(UUID casa, String nomeCasa, UUID fora, String nomeFora, int hs, int as) {
        Partida p = agendada(casa, nomeCasa, fora, nomeFora);
        p.iniciar();
        p.registrarResultado(hs, as, true, "ausência");
        return p;
    }

    @Test
    void tabelaZeradaDesdeOSorteioListaTodasAsEquipes() {
        List<Partida> partidas = List.of(
                agendada(a, "A", b, "B"),
                agendada(a, "A", c, "C"),
                agendada(b, "B", c, "C"));

        List<TeamStanding> tabela = StandingsService.ordenar(partidas, List.of(a, b, c));

        assertThat(tabela).hasSize(3);
        assertThat(tabela).allMatch(l -> l.pontos() == 0 && l.pro() == 0 && l.contra() == 0);
    }

    @Test
    void desempatePorConfrontoDireto() {
        // A e B terminam empatados em pontos (6); o confronto direto A×B decide.
        List<Partida> partidas = new ArrayList<>(List.of(
                jogada(a, "A", b, "B", 1, 0),   // A vence B — confronto direto
                jogada(a, "A", c, "C", 1, 0),   // A vence C
                jogada(d, "D", a, "A", 1, 0),   // A perde pra D
                jogada(b, "B", c, "C", 1, 0),   // B vence C
                jogada(b, "B", d, "D", 1, 0),   // B vence D
                jogada(c, "C", d, "D", 1, 0))); // C vence D
        // A: 6 (venceu B e C, perdeu D); B: 6 (venceu C e D, perdeu A)

        List<TeamStanding> tabela = StandingsService.ordenar(partidas, List.of(a, b, c, d));

        assertThat(tabela.get(0).pontos()).isEqualTo(6);
        assertThat(tabela.get(1).pontos()).isEqualTo(6);
        int posA = indiceDe(tabela, a);
        int posB = indiceDe(tabela, b);
        assertThat(posA).isLessThan(posB); // A acima de B pelo confronto direto
        assertThat(tabela.get(posB).desempate()).isEqualTo("confronto direto");
    }

    @Test
    void woNaoContaNoPlacar() {
        // A vence B por W.O. 1x0: A ganha 3 pts mas Pró/Contra ficam neutros
        List<Partida> partidas = List.of(wo(a, "A", b, "B", 1, 0));

        List<TeamStanding> tabela = StandingsService.ordenar(partidas, List.of(a, b));
        TeamStanding linhaA = tabela.stream().filter(l -> l.teamId().equals(a)).findFirst().orElseThrow();
        TeamStanding linhaB = tabela.stream().filter(l -> l.teamId().equals(b)).findFirst().orElseThrow();

        assertThat(linhaA.pontos()).isEqualTo(3);
        assertThat(linhaA.vitorias()).isEqualTo(1);
        assertThat(linhaA.pro()).isZero();     // neutro no placar
        assertThat(linhaA.contra()).isZero();
        assertThat(linhaA.saldo()).isZero();
        assertThat(linhaB.derrotas()).isEqualTo(1);
        assertThat(linhaB.pro()).isZero();
    }

    private static int indiceDe(List<TeamStanding> tabela, UUID id) {
        for (int i = 0; i < tabela.size(); i++) {
            if (tabela.get(i).teamId().equals(id)) return i;
        }
        return -1;
    }
}
