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
    void quemCriaEODonoEAprovacaoEPadrao() {
        java.util.UUID dono = java.util.UUID.randomUUID();
        Campeonato campeonato = Campeonato.criar("Copa Verão 2026", CampeonatoFormato.PLAYOFFS, dono);

        assertThat(campeonato.semDono()).isFalse();
        assertThat(campeonato.ehDono(dono)).isTrue();
        assertThat(campeonato.ehDono(java.util.UUID.randomUUID())).isFalse();
        assertThat(campeonato.exigeAprovacaoDeInscricoes()).isTrue();
    }

    @Test
    void criaComInscricaoDiretaQuandoOrganizadorEscolhe() {
        Campeonato campeonato = Campeonato.criar("Copa Aberta", CampeonatoFormato.PONTOS_CORRIDOS,
                java.util.UUID.randomUUID(), false);

        assertThat(campeonato.exigeAprovacaoDeInscricoes()).isFalse();
    }

    @Test
    void validaTamanhoDaEquipeQuandoDefinido() {
        Campeonato campeonato = Campeonato.criar("Xadrez Individual", CampeonatoFormato.PONTOS_CORRIDOS,
                java.util.UUID.randomUUID(), true, 1, 1, false);

        assertThat(campeonato.getMinIntegrantes()).isEqualTo(1);
        campeonato.validarNumeroDeIntegrantes(1); // ok
        assertThatThrownBy(() -> campeonato.validarNumeroDeIntegrantes(2))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("máximo");
    }

    @Test
    void semLimiteDeEquipeNaoRestringe() {
        Campeonato campeonato = Campeonato.criar("Aberto", CampeonatoFormato.PONTOS_CORRIDOS);

        campeonato.validarNumeroDeIntegrantes(1);
        campeonato.validarNumeroDeIntegrantes(40); // sem limite = qualquer quantidade
    }

    @Test
    void terceiroLugarSoValeNoMataMata() {
        Campeonato pontos = Campeonato.criar("Liga", CampeonatoFormato.PONTOS_CORRIDOS,
                java.util.UUID.randomUUID(), true, null, null, true);
        Campeonato playoffs = Campeonato.criar("Copa", CampeonatoFormato.PLAYOFFS,
                java.util.UUID.randomUUID(), true, null, null, true);

        assertThat(pontos.temDisputaTerceiro()).isFalse(); // ignorado em pontos corridos
        assertThat(playoffs.temDisputaTerceiro()).isTrue();
    }

    @Test
    void editaSoComOTorneioAberto() {
        Campeonato campeonato = Campeonato.criar("Copa", CampeonatoFormato.PLAYOFFS,
                java.util.UUID.randomUUID(), true, null, null, false);

        campeonato.editar("Copa Renomeada", false, 2, 5, true);
        assertThat(campeonato.getNome()).isEqualTo("Copa Renomeada");
        assertThat(campeonato.exigeAprovacaoDeInscricoes()).isFalse();
        assertThat(campeonato.getMinIntegrantes()).isEqualTo(2);
        assertThat(campeonato.getMaxIntegrantes()).isEqualTo(5);
        assertThat(campeonato.temDisputaTerceiro()).isTrue();

        campeonato.sortear();
        assertThatThrownBy(() -> campeonato.editar("Outro", true, null, null, false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelaDeQualquerEstadoNaoTerminal() {
        Campeonato aberto = Campeonato.criar("Copa", CampeonatoFormato.PLAYOFFS, java.util.UUID.randomUUID());
        aberto.cancelar();
        assertThat(aberto.getStatus()).isEqualTo(CampeonatoStatus.CANCELADO);
        assertThat(aberto.aceitaInscricoes()).isFalse();

        Campeonato emAndamento = Campeonato.criar("Liga", CampeonatoFormato.PLAYOFFS, java.util.UUID.randomUUID());
        emAndamento.sortear();
        emAndamento.iniciar();
        emAndamento.cancelar();
        assertThat(emAndamento.getStatus()).isEqualTo(CampeonatoStatus.CANCELADO);
    }

    @Test
    void naoCancelaTorneioEncerradoNemDuasVezes() {
        Campeonato campeonato = Campeonato.criar("Copa", CampeonatoFormato.PLAYOFFS, java.util.UUID.randomUUID());
        campeonato.sortear();
        campeonato.iniciar();
        campeonato.encerrar(java.util.UUID.randomUUID(), "Timaço FC");
        assertThatThrownBy(campeonato::cancelar).isInstanceOf(IllegalStateException.class);

        Campeonato outro = Campeonato.criar("Liga", CampeonatoFormato.PLAYOFFS, java.util.UUID.randomUUID());
        outro.cancelar();
        assertThatThrownBy(outro::cancelar).isInstanceOf(IllegalStateException.class);
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
