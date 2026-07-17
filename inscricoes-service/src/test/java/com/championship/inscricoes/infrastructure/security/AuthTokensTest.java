package com.championship.inscricoes.infrastructure.security;

import com.championship.inscricoes.application.NaoAutenticadoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthTokensTest {

    private final AuthTokens authTokens = new AuthTokens("segredo-de-teste", new ObjectMapper());

    private HttpServletRequest requestCom(String header) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Authorization")).thenReturn(header);
        return request;
    }

    @Test
    void tokenEmitidoEValidadoDeVolta() {
        UUID usuarioId = UUID.randomUUID();
        String token = authTokens.emitir(usuarioId, "Kaue");

        assertThat(authTokens.exigirUsuario(requestCom("Bearer " + token))).isEqualTo(usuarioId);
    }

    @Test
    void rejeitaTokenAssinadoComOutroSegredo() {
        AuthTokens outroServico = new AuthTokens("outro-segredo", new ObjectMapper());
        String token = outroServico.emitir(UUID.randomUUID(), "Intruso");

        assertThatThrownBy(() -> authTokens.exigirUsuario(requestCom("Bearer " + token)))
                .isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void rejeitaRequisicaoSemToken() {
        assertThatThrownBy(() -> authTokens.exigirUsuario(requestCom(null)))
                .isInstanceOf(NaoAutenticadoException.class);
        assertThat(authTokens.usuarioOpcional(requestCom(null))).isNull();
    }
}
