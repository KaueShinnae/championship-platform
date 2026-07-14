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
        assertThat(campeonato.getFormato()).isEqualTo(CampeonatoFormato.PONTOS_CORRIDOS);
        assertThat(campeonato.aceitaInscricoes()).isTrue();
    }

    @Test
    void criaCampeonatoComFormatoEscolhido() {
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026", CampeonatoFormato.GRUPOS_PLAYOFFS);

        assertThat(campeonato.getFormato()).isEqualTo(CampeonatoFormato.GRUPOS_PLAYOFFS);
    }

    @Test
    void rejeitaNomeMuitoLongo() {
        String nomeGigante = "a".repeat(101);
        assertThatThrownBy(() -> Campeonato.criar(nomeGigante))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cicloDeVidaCompletoAteOCampeao() {
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026", CampeonatoFormato.PLAYOFFS);

        campeonato.sortear();
        assertThat(campeonato.getStatus()).isEqualTo(CampeonatoStatus.SORTEADO);
        assertThat(campeonato.aceitaInscricoes()).isFalse();

        // re-sortear enquanto SORTEADO é permitido
        campeonato.sortear();
        assertThat(campeonato.getStatus()).isEqualTo(CampeonatoStatus.SORTEADO);

        campeonato.iniciar();
        assertThat(campeonato.getStatus()).isEqualTo(CampeonatoStatus.EM_ANDAMENTO);

        java.util.UUID campeaoId = java.util.UUID.randomUUID();
        campeonato.encerrar(campeaoId, "Timaço FC");
        assertThat(campeonato.getStatus()).isEqualTo(CampeonatoStatus.ENCERRADO);
        assertThat(campeonato.getCampeaoTimeId()).isEqualTo(campeaoId);
        assertThat(campeonato.getCampeaoNome()).isEqualTo("Timaço FC");
    }

    @Test
    void reabrirInscricoesVoltaParaAbertoEDescartaOSorteio() {
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026", CampeonatoFormato.PLAYOFFS);
        campeonato.sortear();

        campeonato.reabrirInscricoes();

        assertThat(campeonato.getStatus()).isEqualTo(CampeonatoStatus.ABERTO);
        assertThat(campeonato.aceitaInscricoes()).isTrue();
    }

    @Test
    void bloqueiaTransicoesForaDeOrdem() {
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026", CampeonatoFormato.PLAYOFFS);

        assertThatThrownBy(campeonato::iniciar).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(campeonato::reabrirInscricoes).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> campeonato.encerrar(java.util.UUID.randomUUID(), "Timaço FC"))
                .isInstanceOf(IllegalStateException.class);

        campeonato.sortear();
        campeonato.iniciar();
        assertThatThrownBy(campeonato::sortear).isInstanceOf(IllegalStateException.class);
    }
}
