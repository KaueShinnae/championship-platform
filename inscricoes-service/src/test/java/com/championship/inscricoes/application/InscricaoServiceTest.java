package com.championship.inscricoes.application;

import com.championship.inscricoes.api.TimeDtos.InscricaoDetalheResponse;
import com.championship.inscricoes.domain.Campeonato;
import com.championship.inscricoes.domain.CampeonatoFormato;
import com.championship.inscricoes.domain.Inscricao;
import com.championship.inscricoes.domain.InscricaoStatus;
import com.championship.inscricoes.domain.Time;
import com.championship.inscricoes.infrastructure.messaging.DomainEventWriter;
import com.championship.inscricoes.infrastructure.messaging.events.ChampionshipPermissionsChangedPayload;
import com.championship.inscricoes.infrastructure.messaging.events.TeamRegisteredPayload;
import com.championship.inscricoes.infrastructure.persistence.CampeonatoAdminRepository;
import com.championship.inscricoes.infrastructure.persistence.CampeonatoRepository;
import com.championship.inscricoes.infrastructure.persistence.GestaoLogRepository;
import com.championship.inscricoes.infrastructure.persistence.InscricaoRepository;
import com.championship.inscricoes.infrastructure.persistence.TimeRepository;
import com.championship.inscricoes.infrastructure.persistence.UsuarioRepository;
import com.championship.inscricoes.infrastructure.security.AuthTokens.Sessao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscricaoServiceTest {

    @Mock
    private CampeonatoRepository campeonatoRepository;

    @Mock
    private TimeRepository timeRepository;

    @Mock
    private InscricaoRepository inscricaoRepository;

    @Mock
    private DomainEventWriter domainEventWriter;

    @Mock
    private CampeonatoAdminRepository campeonatoAdminRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private GestaoLogRepository gestaoLogRepository;

    @InjectMocks
    private InscricaoService inscricaoService;

    private static Sessao ator(UUID id) {
        return new Sessao(id, "Fulano");
    }

    private Campeonato campeonatoComDono(UUID donoId) {
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026", CampeonatoFormato.PONTOS_CORRIDOS, donoId);
        when(campeonatoRepository.findById(campeonato.getId())).thenReturn(Optional.of(campeonato));
        return campeonato;
    }

    @Test
    void rejeitaTimeDuplicadoNoMesmoCampeonato() {
        UUID donoId = UUID.randomUUID();
        Campeonato campeonato = campeonatoComDono(donoId);
        when(inscricaoRepository.existsAtivaByCampeonatoIdAndNomeTime(campeonato.getId(), "Timaço FC"))
                .thenReturn(true);

        assertThatThrownBy(() -> inscricaoService.inscreverTime(
                        campeonato.getId(), donoId, "Timaço FC", List.of("Jogador 1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ja esta inscrito");
    }

    @Test
    void bloqueiaGestaoDeQuemNaoEDonoNemAdmin() {
        UUID intruso = UUID.randomUUID();
        Campeonato campeonato = campeonatoComDono(UUID.randomUUID());
        when(campeonatoAdminRepository.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> inscricaoService.marcarSorteado(campeonato.getId(), intruso))
                .isInstanceOf(SemPermissaoException.class);
    }

    @Test
    void adminDelegadoPodeGerenciar() {
        Campeonato campeonato = campeonatoComDono(UUID.randomUUID());
        when(campeonatoAdminRepository.existsById(any())).thenReturn(true);
        when(campeonatoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(inscricaoService.marcarSorteado(campeonato.getId(), UUID.randomUUID()).getStatus().name())
                .isEqualTo("SORTEADO");
    }

    @Test
    void apenasODonoDelegaAdministradores() {
        Campeonato campeonato = campeonatoComDono(UUID.randomUUID());

        assertThatThrownBy(() -> inscricaoService.adicionarAdmin(campeonato.getId(), UUID.randomUUID(), "x@y.com"))
                .isInstanceOf(SemPermissaoException.class);
    }

    @Test
    void apenasODonoRemoveAdministradores() {
        Campeonato campeonato = campeonatoComDono(UUID.randomUUID());

        assertThatThrownBy(() -> inscricaoService.removerAdmin(campeonato.getId(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(SemPermissaoException.class);
    }

    @Test
    void listarCampeonatosDelegaAoRepositorio() {
        when(campeonatoRepository.findAll()).thenReturn(List.of(Campeonato.criar("Copa Verão 2026")));

        assertThat(inscricaoService.listarCampeonatos()).hasSize(1);
    }

    @Test
    void listarInscricoesRetornaDetalheComJogadoresEStatusDaSaga() {
        UUID campeonatoId = UUID.randomUUID();
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026");
        Time time = Time.criar("Timaço FC", List.of("Jogador 1", "Jogador 2"));
        Inscricao inscricao = Inscricao.pendente(time, campeonato);
        when(inscricaoRepository.findDetalhadoByCampeonatoId(campeonatoId)).thenReturn(List.of(inscricao));

        List<Inscricao> inscricoes = inscricaoService.listarInscricoes(campeonatoId);
        InscricaoDetalheResponse detalhe = InscricaoDetalheResponse.from(inscricoes.get(0));

        assertThat(detalhe.timeNome()).isEqualTo("Timaço FC");
        assertThat(detalhe.jogadores()).hasSize(2);
        assertThat(detalhe.status()).isEqualTo(InscricaoStatus.PENDENTE);
        assertThat(detalhe.capitaoUsuarioId()).isNull();
        assertThat(detalhe.confirmedAt()).isNull();
    }

    // ---- eventos de permissão (projeção do partidas-service) ----

    @Test
    void criarCampeonatoEmiteSnapshotDePermissoes() {
        UUID donoId = UUID.randomUUID();
        when(campeonatoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Campeonato criado = inscricaoService.criarCampeonato("Copa Verão 2026",
                CampeonatoFormato.PONTOS_CORRIDOS, donoId, true);

        verify(domainEventWriter).write(eq(criado.getId()),
                eq(ChampionshipPermissionsChangedPayload.TYPE), any());
    }

    // ---- auto-inscrição pelo capitão ----

    @Test
    void organizadorInscreveTimeEDisparaASagaNaHora() {
        UUID donoId = UUID.randomUUID();
        Campeonato campeonato = campeonatoComDono(donoId);
        when(timeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inscricaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Inscricao inscricao = inscricaoService.inscreverTime(
                campeonato.getId(), donoId, "Timaço FC", List.of("Jogador 1"));

        assertThat(inscricao.getCapitaoUsuarioId()).isNull();
        verify(domainEventWriter).write(any(), eq(TeamRegisteredPayload.TYPE), any());
    }

    @Test
    void capitaoFicaPendenteSemDispararEvento() {
        UUID capitao = UUID.randomUUID();
        Campeonato campeonato = campeonatoComDono(UUID.randomUUID());
        when(campeonatoAdminRepository.existsById(any())).thenReturn(false);
        when(timeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inscricaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Inscricao inscricao = inscricaoService.inscreverTime(
                campeonato.getId(), capitao, "Meu Time", List.of("Jogador 1"));

        assertThat(inscricao.getStatus()).isEqualTo(InscricaoStatus.PENDENTE);
        assertThat(inscricao.getCapitaoUsuarioId()).isEqualTo(capitao);
        verify(domainEventWriter, never()).write(any(), any(), any());
    }

    @Test
    void capitaoEntraDiretoQuandoOTorneioNaoExigeAprovacao() {
        UUID capitao = UUID.randomUUID();
        Campeonato campeonato = Campeonato.criar("Copa Aberta", CampeonatoFormato.PONTOS_CORRIDOS,
                UUID.randomUUID(), false);
        when(campeonatoRepository.findById(campeonato.getId())).thenReturn(Optional.of(campeonato));
        when(campeonatoAdminRepository.existsById(any())).thenReturn(false);
        when(timeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inscricaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Inscricao inscricao = inscricaoService.inscreverTime(
                campeonato.getId(), capitao, "Meu Time", List.of("Jogador 1"));

        // modo direto: a saga de confirmação dispara na hora
        assertThat(inscricao.getCapitaoUsuarioId()).isEqualTo(capitao);
        verify(domainEventWriter).write(any(), eq(TeamRegisteredPayload.TYPE), any());
    }

    @Test
    void gestorRemoveTimeEnquantoInscricoesAbertas() {
        UUID donoId = UUID.randomUUID();
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026", CampeonatoFormato.PONTOS_CORRIDOS, donoId);
        Time time = Time.criar("Indesejado", List.of("Jogador 1"), UUID.randomUUID());
        Inscricao inscricao = Inscricao.pendenteDeCapitao(time, campeonato, UUID.randomUUID());
        inscricao.confirmar();
        when(inscricaoRepository.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

        inscricaoService.removerInscricao(campeonato.getId(), inscricao.getId(), ator(donoId));

        verify(inscricaoRepository).delete(inscricao);
        verify(timeRepository).delete(time);
    }

    @Test
    void gestorNaoRemoveTimeAposOSorteio() {
        UUID donoId = UUID.randomUUID();
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026", CampeonatoFormato.PONTOS_CORRIDOS, donoId);
        campeonato.sortear();
        Time time = Time.criar("Timaço FC", List.of("Jogador 1"), UUID.randomUUID());
        Inscricao inscricao = Inscricao.pendenteDeCapitao(time, campeonato, UUID.randomUUID());
        when(inscricaoRepository.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

        assertThatThrownBy(() -> inscricaoService.removerInscricao(campeonato.getId(), inscricao.getId(), ator(donoId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inscricoes abertas");
    }

    @Test
    void capitaoNaoAcumulaDuasInscricoesPendentes() {
        UUID capitao = UUID.randomUUID();
        Campeonato campeonato = campeonatoComDono(UUID.randomUUID());
        when(campeonatoAdminRepository.existsById(any())).thenReturn(false);
        when(inscricaoRepository.existsPendenteDoCapitao(campeonato.getId(), capitao)).thenReturn(true);

        assertThatThrownBy(() -> inscricaoService.inscreverTime(
                        campeonato.getId(), capitao, "Outro Time", List.of("Jogador 1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aguardando aprovacao");
    }

    @Test
    void aprovarInscricaoDoCapitaoDisparaASagaDeConfirmacao() {
        UUID donoId = UUID.randomUUID();
        Campeonato campeonato = campeonatoComDono(donoId);
        Time time = Time.criar("Meu Time", List.of("Jogador 1"), UUID.randomUUID());
        Inscricao inscricao = Inscricao.pendenteDeCapitao(time, campeonato, UUID.randomUUID());
        when(inscricaoRepository.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

        inscricaoService.aprovarInscricao(campeonato.getId(), inscricao.getId(), ator(donoId));

        verify(domainEventWriter).write(eq(time.getId()), eq(TeamRegisteredPayload.TYPE), any());
    }

    @Test
    void recusarDeixaInscricaoRecusadaSemEvento() {
        UUID donoId = UUID.randomUUID();
        Campeonato campeonato = campeonatoComDono(donoId);
        Time time = Time.criar("Meu Time", List.of("Jogador 1"), UUID.randomUUID());
        Inscricao inscricao = Inscricao.pendenteDeCapitao(time, campeonato, UUID.randomUUID());
        when(inscricaoRepository.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));
        when(inscricaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Inscricao recusada = inscricaoService.recusarInscricao(campeonato.getId(), inscricao.getId(), ator(donoId));

        assertThat(recusada.getStatus()).isEqualTo(InscricaoStatus.RECUSADA);
        verify(domainEventWriter, never()).write(any(), eq(TeamRegisteredPayload.TYPE), any());
    }

    @Test
    void capitaoCancelaAPropriaInscricaoPendente() {
        UUID capitao = UUID.randomUUID();
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026", CampeonatoFormato.PONTOS_CORRIDOS,
                UUID.randomUUID());
        Time time = Time.criar("Meu Time", List.of("Jogador 1"), capitao);
        Inscricao inscricao = Inscricao.pendenteDeCapitao(time, campeonato, capitao);
        when(inscricaoRepository.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));
        when(campeonatoAdminRepository.existsById(any())).thenReturn(false);

        inscricaoService.removerInscricao(campeonato.getId(), inscricao.getId(), ator(capitao));

        verify(inscricaoRepository).delete(inscricao);
        verify(timeRepository).delete(time);
    }

    @Test
    void estranhoNaoRemoveInscricaoDeOutroCapitao() {
        UUID capitao = UUID.randomUUID();
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026", CampeonatoFormato.PONTOS_CORRIDOS,
                UUID.randomUUID());
        Inscricao inscricao = Inscricao.pendenteDeCapitao(
                Time.criar("Meu Time", List.of("Jogador 1"), capitao), campeonato, capitao);
        when(inscricaoRepository.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));
        when(campeonatoAdminRepository.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> inscricaoService.removerInscricao(
                        campeonato.getId(), inscricao.getId(), ator(UUID.randomUUID())))
                .isInstanceOf(SemPermissaoException.class);
    }

    // ---- reuso de elenco ("Meus times") ----

    @Test
    void meusTimesDedupePorNomeMantendoOMaisRecente() {
        UUID usuarioId = UUID.randomUUID();
        Time recente = Time.criar("Alpha", List.of("Novo 1", "Novo 2"), usuarioId);
        Time antigo = Time.criar("alpha", List.of("Velho 1"), usuarioId);
        Time outro = Time.criar("Beta", List.of("Jogador 1"), usuarioId);
        when(timeRepository.findReutilizaveisPor(usuarioId)).thenReturn(List.of(recente, antigo, outro));

        List<InscricaoService.TimeReutilizavel> sugestoes = inscricaoService.listarTimesReutilizaveis(usuarioId);

        assertThat(sugestoes).hasSize(2);
        assertThat(sugestoes.get(0).nome()).isEqualTo("Alpha");
        assertThat(sugestoes.get(0).jogadores()).containsExactly("Novo 1", "Novo 2");
        assertThat(sugestoes.get(1).nome()).isEqualTo("Beta");
    }
}
