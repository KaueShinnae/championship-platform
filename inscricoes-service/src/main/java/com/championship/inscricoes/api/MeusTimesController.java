package com.championship.inscricoes.api;

import com.championship.inscricoes.application.InscricaoService;
import com.championship.inscricoes.application.InscricaoService.TimeReutilizavel;
import com.championship.inscricoes.infrastructure.security.AuthTokens;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Sugestões de reuso de elenco ("Meus times"): times que o usuário cadastrou
 * ou que estão em torneios que ele gerencia. O reuso é sempre snapshot — o
 * dashboard copia nome/elenco para uma inscrição nova.
 */
@RestController
@RequestMapping("/meus-times")
public class MeusTimesController {

    private final InscricaoService inscricaoService;
    private final AuthTokens authTokens;

    public MeusTimesController(InscricaoService inscricaoService, AuthTokens authTokens) {
        this.inscricaoService = inscricaoService;
        this.authTokens = authTokens;
    }

    @GetMapping
    public List<TimeReutilizavel> listar(HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        return inscricaoService.listarTimesReutilizaveis(usuarioId);
    }
}
