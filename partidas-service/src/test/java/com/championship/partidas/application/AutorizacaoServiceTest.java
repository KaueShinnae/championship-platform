package com.championship.partidas.application;

import com.championship.partidas.infrastructure.persistence.CampeonatoPermissao.CampeonatoPermissaoId;
import com.championship.partidas.infrastructure.persistence.CampeonatoPermissaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacaoServiceTest {

    @Mock
    private CampeonatoPermissaoRepository permissaoRepository;

    @InjectMocks
    private AutorizacaoService autorizacaoService;

    private final UUID campeonatoId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void semProjecaoQualquerAutenticadoGerencia() {
        // legado sem dono ou janela de propagação do evento — regra de produto
        when(permissaoRepository.existsByIdCampeonatoId(campeonatoId)).thenReturn(false);

        assertThatCode(() -> autorizacaoService.exigirGestor(campeonatoId, usuarioId))
                .doesNotThrowAnyException();
    }

    @Test
    void comProjecaoEhFailClosedParaQuemNaoEGestor() {
        when(permissaoRepository.existsByIdCampeonatoId(campeonatoId)).thenReturn(true);
        when(permissaoRepository.existsById(new CampeonatoPermissaoId(campeonatoId, usuarioId)))
                .thenReturn(false);

        assertThatThrownBy(() -> autorizacaoService.exigirGestor(campeonatoId, usuarioId))
                .isInstanceOf(SemPermissaoException.class);
    }

    @Test
    void donoOuAdminNaProjecaoGerencia() {
        when(permissaoRepository.existsByIdCampeonatoId(campeonatoId)).thenReturn(true);
        when(permissaoRepository.existsById(new CampeonatoPermissaoId(campeonatoId, usuarioId)))
                .thenReturn(true);

        assertThatCode(() -> autorizacaoService.exigirGestor(campeonatoId, usuarioId))
                .doesNotThrowAnyException();
    }
}
