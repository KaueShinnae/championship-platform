package com.championship.inscricoes.api;

import com.championship.inscricoes.api.TimeDtos.InscreverTimeRequest;
import com.championship.inscricoes.api.TimeDtos.InscricaoDetalheResponse;
import com.championship.inscricoes.api.TimeDtos.InscricaoResponse;
import com.championship.inscricoes.application.InscricaoService;
import com.championship.inscricoes.domain.Inscricao;
import com.championship.inscricoes.infrastructure.security.AuthTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/campeonatos/{campeonatoId}/times")
public class TimeController {

    private final InscricaoService inscricaoService;
    private final AuthTokens authTokens;

    public TimeController(InscricaoService inscricaoService, AuthTokens authTokens) {
        this.inscricaoService = inscricaoService;
        this.authTokens = authTokens;
    }

    @PostMapping
    public ResponseEntity<InscricaoResponse> inscrever(@PathVariable UUID campeonatoId,
                                                         @Valid @RequestBody InscreverTimeRequest request,
                                                         HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        Inscricao inscricao = inscricaoService.inscreverTime(campeonatoId, usuarioId, request.nome(), request.jogadores());
        return ResponseEntity.status(201).body(InscricaoResponse.from(inscricao));
    }

    @GetMapping
    public List<InscricaoDetalheResponse> listar(@PathVariable UUID campeonatoId) {
        return inscricaoService.listarInscricoes(campeonatoId).stream()
                .map(InscricaoDetalheResponse::from)
                .toList();
    }

    @PutMapping("/{inscricaoId}")
    public InscricaoDetalheResponse editar(@PathVariable UUID campeonatoId, @PathVariable UUID inscricaoId,
                                            @Valid @RequestBody InscreverTimeRequest request,
                                            HttpServletRequest http) {
        Inscricao inscricao = inscricaoService.editarTime(
                campeonatoId, inscricaoId, request.nome(), request.jogadores(), authTokens.sessao(http));
        return InscricaoDetalheResponse.from(inscricao);
    }

    @PostMapping("/{inscricaoId}/aprovar")
    public InscricaoResponse aprovar(@PathVariable UUID campeonatoId, @PathVariable UUID inscricaoId,
                                      HttpServletRequest http) {
        return InscricaoResponse.from(
                inscricaoService.aprovarInscricao(campeonatoId, inscricaoId, authTokens.sessao(http)));
    }

    @PostMapping("/{inscricaoId}/recusar")
    public InscricaoResponse recusar(@PathVariable UUID campeonatoId, @PathVariable UUID inscricaoId,
                                      HttpServletRequest http) {
        return InscricaoResponse.from(
                inscricaoService.recusarInscricao(campeonatoId, inscricaoId, authTokens.sessao(http)));
    }

    @DeleteMapping("/{inscricaoId}")
    public ResponseEntity<Void> remover(@PathVariable UUID campeonatoId, @PathVariable UUID inscricaoId,
                                         HttpServletRequest http) {
        inscricaoService.removerInscricao(campeonatoId, inscricaoId, authTokens.sessao(http));
        return ResponseEntity.noContent().build();
    }
}
