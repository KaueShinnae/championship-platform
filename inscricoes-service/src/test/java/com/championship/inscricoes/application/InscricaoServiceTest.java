package com.championship.inscricoes.application;

import com.championship.inscricoes.api.TimeDtos.InscricaoDetalheResponse;
import com.championship.inscricoes.domain.Campeonato;
import com.championship.inscricoes.domain.Inscricao;
import com.championship.inscricoes.domain.InscricaoStatus;
import com.championship.inscricoes.domain.Time;
import com.championship.inscricoes.infrastructure.messaging.DomainEventWriter;
import com.championship.inscricoes.infrastructure.persistence.CampeonatoRepository;
import com.championship.inscricoes.infrastructure.persistence.InscricaoRepository;
import com.championship.inscricoes.infrastructure.persistence.TimeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @InjectMocks
    private InscricaoService inscricaoService;

    @Test
    void rejeitaTimeDuplicadoNoMesmoCampeonato() {
        UUID campeonatoId = UUID.randomUUID();
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026");
        when(campeonatoRepository.findById(campeonatoId)).thenReturn(java.util.Optional.of(campeonato));
        when(inscricaoRepository.existsByCampeonatoIdAndNomeTime(campeonatoId, "Timaço FC")).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> inscricaoService.inscreverTime(campeonatoId, "Timaço FC", List.of("Jogador 1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ja esta inscrito");
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
        assertThat(detalhe.confirmedAt()).isNull();
    }
}
