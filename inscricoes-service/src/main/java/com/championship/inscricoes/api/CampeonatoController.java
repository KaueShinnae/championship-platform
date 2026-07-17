package com.championship.inscricoes.api;

import com.championship.inscricoes.api.AuthController.UsuarioView;
import com.championship.inscricoes.api.CampeonatoDtos.AdicionarAdminRequest;
import com.championship.inscricoes.api.CampeonatoDtos.CampeonatoResponse;
import com.championship.inscricoes.api.CampeonatoDtos.CriarCampeonatoRequest;
import com.championship.inscricoes.application.InscricaoService;
import com.championship.inscricoes.domain.Campeonato;
import com.championship.inscricoes.domain.Usuario;
import com.championship.inscricoes.infrastructure.security.AuthTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/campeonatos")
public class CampeonatoController {

    private final InscricaoService inscricaoService;
    private final AuthTokens authTokens;

    public CampeonatoController(InscricaoService inscricaoService, AuthTokens authTokens) {
        this.inscricaoService = inscricaoService;
        this.authTokens = authTokens;
    }

    @PostMapping
    public ResponseEntity<CampeonatoResponse> criar(@Valid @RequestBody CriarCampeonatoRequest request,
                                                     HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        boolean aprovacao = request.aprovacaoInscricoes() == null || request.aprovacaoInscricoes();
        Campeonato campeonato = inscricaoService.criarCampeonato(
                request.nome(), request.formato(), usuarioId, aprovacao);
        return ResponseEntity.status(201).body(CampeonatoResponse.from(campeonato, true, true));
    }

    @GetMapping
    public List<CampeonatoResponse> listar(HttpServletRequest http) {
        UUID usuarioId = authTokens.usuarioOpcional(http);
        return inscricaoService.listarCampeonatos().stream()
                .map(campeonato -> toResponse(campeonato, usuarioId))
                .toList();
    }

    @PostMapping("/{campeonatoId}/sortear")
    public CampeonatoResponse sortear(@PathVariable UUID campeonatoId, HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        return toResponse(inscricaoService.marcarSorteado(campeonatoId, usuarioId), usuarioId);
    }

    @PostMapping("/{campeonatoId}/reabrir")
    public CampeonatoResponse reabrir(@PathVariable UUID campeonatoId, HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        return toResponse(inscricaoService.reabrirInscricoes(campeonatoId, usuarioId), usuarioId);
    }

    @PostMapping("/{campeonatoId}/iniciar")
    public CampeonatoResponse iniciar(@PathVariable UUID campeonatoId, HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        return toResponse(inscricaoService.iniciarCampeonato(campeonatoId, usuarioId), usuarioId);
    }

    @PostMapping("/{campeonatoId}/admins")
    public ResponseEntity<UsuarioView> adicionarAdmin(@PathVariable UUID campeonatoId,
                                                       @Valid @RequestBody AdicionarAdminRequest request,
                                                       HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        Usuario admin = inscricaoService.adicionarAdmin(campeonatoId, usuarioId, request.email());
        return ResponseEntity.status(201).body(new UsuarioView(admin.getId(), admin.getNome(), admin.getEmail()));
    }

    @DeleteMapping("/{campeonatoId}/admins/{adminId}")
    public ResponseEntity<Void> removerAdmin(@PathVariable UUID campeonatoId, @PathVariable UUID adminId,
                                              HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        inscricaoService.removerAdmin(campeonatoId, usuarioId, adminId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{campeonatoId}/admins")
    public List<UsuarioView> listarAdmins(@PathVariable UUID campeonatoId) {
        return inscricaoService.listarAdmins(campeonatoId).stream()
                .map(admin -> new UsuarioView(admin.getId(), admin.getNome(), admin.getEmail()))
                .toList();
    }

    private CampeonatoResponse toResponse(Campeonato campeonato, UUID usuarioId) {
        boolean canManage = inscricaoService.podeGerenciar(campeonato, usuarioId);
        boolean isDono = usuarioId != null && campeonato.ehDono(usuarioId);
        return CampeonatoResponse.from(campeonato, canManage, isDono);
    }
}
