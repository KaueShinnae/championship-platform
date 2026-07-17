package com.championship.partidas.application;

import com.championship.partidas.domain.FormatoTorneio;
import com.championship.partidas.domain.Partida;
import com.championship.partidas.domain.PartidaStage;
import com.championship.partidas.infrastructure.messaging.DomainEventWriter;
import com.championship.partidas.infrastructure.messaging.events.ChampionshipCompletedPayload;
import com.championship.partidas.infrastructure.messaging.events.TeamRef;
import com.championship.partidas.infrastructure.persistence.ChaveSlot;
import com.championship.partidas.infrastructure.persistence.ChaveSlotRepository;
import com.championship.partidas.infrastructure.persistence.PartidaRepository;
import com.championship.partidas.infrastructure.persistence.TorneioChaveamento;
import com.championship.partidas.infrastructure.persistence.TorneioChaveamentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChaveamentoServiceTest {

    @Mock
    private PartidaRepository partidaRepository;

    @Mock
    private TorneioChaveamentoRepository chaveamentoRepository;

    @Mock
    private ChaveSlotRepository chaveSlotRepository;

    @Mock
    private DomainEventWriter domainEventWriter;

    private ChaveamentoService service;

    private final List<Partida> partidasSalvas = new ArrayList<>();
    private final Map<ChaveSlot.ChaveSlotId, ChaveSlot> slots = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new ChaveamentoService(partidaRepository, chaveamentoRepository,
                chaveSlotRepository, domainEventWriter, new ObjectMapper());

        // repositórios com estado em memória: o pareamento de slots durante a
        // geração depende de ler o que acabou de ser salvo
        lenient().when(partidaRepository.save(any())).thenAnswer(invocation -> {
            Partida partida = invocation.getArgument(0);
            partidasSalvas.add(partida);
            return partida;
        });
        lenient().when(chaveSlotRepository.save(any())).thenAnswer(invocation -> {
            ChaveSlot slot = invocation.getArgument(0);
            slots.put(slot.getId(), slot);
            return slot;
        });
        lenient().when(chaveSlotRepository.findById(any()))
                .thenAnswer(invocation -> Optional.ofNullable(slots.get(invocation.getArgument(0))));
        lenient().when(chaveSlotRepository.findByIdCampeonatoId(any()))
                .thenAnswer(invocation -> List.copyOf(slots.values()));
        lenient().when(partidaRepository.findByCampeonatoId(any())).thenReturn(List.of());
    }

    private static List<TeamRef> times(int quantidade) {
        List<TeamRef> times = new ArrayList<>();
        for (int i = 1; i <= quantidade; i++) {
            times.add(new TeamRef(UUID.randomUUID(), "Time " + i));
        }
        return times;
    }

    @Test
    void pontosCorridosGeraTodosContraTodosNumGrupoUnico() {
        List<Partida> criadas = service.gerar(UUID.randomUUID(), FormatoTorneio.PONTOS_CORRIDOS, times(4));

        assertThat(criadas).hasSize(6); // 4*3/2
        assertThat(criadas).allMatch(partida -> partida.getStage() == PartidaStage.GRUPOS);
        assertThat(criadas.stream().map(Partida::getGroupId).distinct()).hasSize(1);
    }

    @Test
    void gruposPlayoffsCom8TimesGera2GruposDe4() {
        List<Partida> criadas = service.gerar(UUID.randomUUID(), FormatoTorneio.GRUPOS_PLAYOFFS, times(8));

        assertThat(criadas).hasSize(12); // 2 grupos de 4 -> 6 partidas cada
        assertThat(criadas.stream().map(Partida::getGroupId).distinct()).hasSize(2);
        // playoffs ainda não existem: são semeados quando os grupos terminarem
        assertThat(criadas).noneMatch(partida -> partida.getStage() == PartidaStage.PLAYOFF);
    }

    @Test
    void playoffsCom5TimesDao3ByesEUmaPartidaDePrimeiraRodada() {
        List<Partida> criadas = service.gerar(UUID.randomUUID(), FormatoTorneio.PLAYOFFS, times(5));

        // 2 times jogam a 1ª rodada; 3 byes entram na 2ª — dois deles caem no
        // mesmo confronto (slots 0 e 1) e a partida da 2ª rodada já nasce
        List<Partida> rodada1 = criadas.stream().filter(partida -> partida.getRound() == 1).toList();
        List<Partida> rodada2 = criadas.stream().filter(partida -> partida.getRound() == 2).toList();
        assertThat(rodada1).hasSize(1);
        assertThat(rodada2).hasSize(1);
        assertThat(criadas).allMatch(partida -> partida.getStage() == PartidaStage.PLAYOFF);
        // partidas geradas nascem sem horario ("a definir")
        assertThat(criadas).allMatch(partida -> partida.getScheduledAt() == null);
        // os 3 byes ficam visiveis como slots da 2ª rodada (bye nominal no bracket)
        assertThat(service.listarSlots(criadas.get(0).getCampeonatoId()).stream()
                .filter(slot -> slot.getId().getRound() == 2))
                .hasSize(3);
    }

    @Test
    void rejeitaSorteioComMenosTimesQueOMinimoDoFormato() {
        assertThatThrownBy(() -> service.gerar(UUID.randomUUID(), FormatoTorneio.GRUPOS_PLAYOFFS, times(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("6");
    }

    @Test
    void naoDescartaSorteioDeTorneioJaIniciado() {
        UUID campeonatoId = UUID.randomUUID();
        Partida emAndamento = Partida.agendar(campeonatoId, UUID.randomUUID(),
                UUID.randomUUID(), "Timaço FC", UUID.randomUUID(), "Rival FC", null);
        emAndamento.iniciar();
        when(partidaRepository.findByCampeonatoId(campeonatoId)).thenReturn(List.of(emAndamento));

        assertThatThrownBy(() -> service.descartarSorteio(campeonatoId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void vencedorDaFinalPublicaChampionshipCompleted() {
        UUID campeonatoId = UUID.randomUUID();
        when(chaveamentoRepository.findById(campeonatoId)).thenReturn(Optional.of(
                new TorneioChaveamento(campeonatoId, FormatoTorneio.PLAYOFFS, 1, "[]", null)));

        Partida finalDoTorneio = Partida.dePlayoff(campeonatoId, 1, 0,
                UUID.randomUUID(), "Timaço FC", UUID.randomUUID(), "Rival FC");
        finalDoTorneio.iniciar();
        finalDoTorneio.registrarResultado(3, 1);

        service.aoFinalizar(finalDoTorneio);

        verify(domainEventWriter).write(eq(campeonatoId), eq(ChampionshipCompletedPayload.TYPE), any());
    }

    @Test
    void vencedorDeRodadaIntermediariaOcupaSlotECriaProximaPartidaQuandoOParEstaCompleto() {
        UUID campeonatoId = UUID.randomUUID();
        when(chaveamentoRepository.findById(campeonatoId)).thenReturn(Optional.of(
                new TorneioChaveamento(campeonatoId, FormatoTorneio.PLAYOFFS, 2, "[]", null)));

        Partida semi1 = Partida.dePlayoff(campeonatoId, 1, 0,
                UUID.randomUUID(), "Timaço FC", UUID.randomUUID(), "Rival FC");
        semi1.iniciar();
        semi1.registrarResultado(2, 0);
        service.aoFinalizar(semi1);

        // só um finalista definido: final ainda não existe
        assertThat(partidasSalvas).isEmpty();
        verify(domainEventWriter, never()).write(any(), eq(ChampionshipCompletedPayload.TYPE), any());

        Partida semi2 = Partida.dePlayoff(campeonatoId, 1, 1,
                UUID.randomUUID(), "Águias EC", UUID.randomUUID(), "Falcões FC");
        semi2.iniciar();
        semi2.registrarResultado(0, 1);
        service.aoFinalizar(semi2);

        assertThat(partidasSalvas).hasSize(1);
        Partida partidaFinal = partidasSalvas.get(0);
        assertThat(partidaFinal.getRound()).isEqualTo(2);
        assertThat(partidaFinal.getHomeTeamName()).isEqualTo("Timaço FC");
        assertThat(partidaFinal.getAwayTeamName()).isEqualTo("Falcões FC");
    }

    @Test
    void ultimaPartidaDosPontosCorridosDefineOCampeaoPelaClassificacao() throws Exception {
        UUID campeonatoId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        TeamRef lider = new TeamRef(UUID.randomUUID(), "Timaço FC");
        TeamRef segundo = new TeamRef(UUID.randomUUID(), "Rival FC");
        TeamRef terceiro = new TeamRef(UUID.randomUUID(), "Águias EC");

        Partida jogo1 = finalizada(campeonatoId, groupId, lider, segundo, 2, 0);
        Partida jogo2 = finalizada(campeonatoId, groupId, lider, terceiro, 1, 0);
        Partida jogo3 = finalizada(campeonatoId, groupId, segundo, terceiro, 1, 1);

        String drawOrder = new ObjectMapper().writeValueAsString(
                List.of(lider.teamId().toString(), segundo.teamId().toString(), terceiro.teamId().toString()));
        when(chaveamentoRepository.findById(campeonatoId)).thenReturn(Optional.of(
                new TorneioChaveamento(campeonatoId, FormatoTorneio.PONTOS_CORRIDOS, null, drawOrder, null)));
        when(partidaRepository.findByCampeonatoId(campeonatoId)).thenReturn(List.of(jogo1, jogo2, jogo3));

        service.aoFinalizar(jogo3);

        verify(domainEventWriter).write(eq(campeonatoId), eq(ChampionshipCompletedPayload.TYPE),
                any(ChampionshipCompletedPayload.class));
    }

    @Test
    void fimDaFaseDeGruposSemeiaPlayoffsComSeedingCruzado() throws Exception {
        UUID campeonatoId = UUID.randomUUID();
        UUID grupoA = UUID.randomUUID();
        UUID grupoB = UUID.randomUUID();
        // grupo A: A1 > A2 > A3; grupo B: B1 > B2 > B3
        TeamRef a1 = new TeamRef(UUID.randomUUID(), "A1");
        TeamRef a2 = new TeamRef(UUID.randomUUID(), "A2");
        TeamRef a3 = new TeamRef(UUID.randomUUID(), "A3");
        TeamRef b1 = new TeamRef(UUID.randomUUID(), "B1");
        TeamRef b2 = new TeamRef(UUID.randomUUID(), "B2");
        TeamRef b3 = new TeamRef(UUID.randomUUID(), "B3");

        List<Partida> partidas = List.of(
                finalizada(campeonatoId, grupoA, a1, a2, 2, 0),
                finalizada(campeonatoId, grupoA, a1, a3, 3, 0),
                finalizada(campeonatoId, grupoA, a2, a3, 1, 0),
                finalizada(campeonatoId, grupoB, b1, b2, 2, 0),
                finalizada(campeonatoId, grupoB, b1, b3, 3, 0),
                finalizada(campeonatoId, grupoB, b2, b3, 1, 0));

        ObjectMapper mapper = new ObjectMapper();
        String drawOrder = mapper.writeValueAsString(List.of(
                a1.teamId().toString(), b1.teamId().toString(), a2.teamId().toString(),
                b2.teamId().toString(), a3.teamId().toString(), b3.teamId().toString()));
        String groupIds = mapper.writeValueAsString(List.of(grupoA.toString(), grupoB.toString()));

        when(chaveamentoRepository.findById(campeonatoId)).thenReturn(Optional.of(
                new TorneioChaveamento(campeonatoId, FormatoTorneio.GRUPOS_PLAYOFFS, 2, drawOrder, groupIds)));
        when(partidaRepository.findByCampeonatoId(campeonatoId)).thenReturn(partidas);

        service.aoFinalizar(partidas.get(5));

        // semifinais: 1ºA x 2ºB e 1ºB x 2ºA
        assertThat(partidasSalvas).hasSize(2);
        Partida semi1 = partidasSalvas.get(0);
        Partida semi2 = partidasSalvas.get(1);
        assertThat(semi1.getHomeTeamName()).isEqualTo("A1");
        assertThat(semi1.getAwayTeamName()).isEqualTo("B2");
        assertThat(semi2.getHomeTeamName()).isEqualTo("B1");
        assertThat(semi2.getAwayTeamName()).isEqualTo("A2");
        assertThat(partidasSalvas).allMatch(partida -> partida.getRound() == 1);
    }

    private static Partida finalizada(UUID campeonatoId, UUID groupId,
                                       TeamRef casa, TeamRef fora, int golsCasa, int golsFora) {
        Partida partida = Partida.agendar(campeonatoId, groupId,
                casa.teamId(), casa.name(), fora.teamId(), fora.name(), null);
        partida.iniciar();
        partida.registrarResultado(golsCasa, golsFora);
        return partida;
    }
}
