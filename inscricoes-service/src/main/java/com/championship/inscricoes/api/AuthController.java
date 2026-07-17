package com.championship.inscricoes.api;

import com.championship.inscricoes.application.AuthService;
import com.championship.inscricoes.application.AuthService.Sessao;
import com.championship.inscricoes.domain.Usuario;
import com.championship.inscricoes.infrastructure.security.AuthTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record RegistrarRequest(
            @NotBlank @Size(max = 100) String nome,
            @NotBlank @Size(max = 150) String email,
            @NotBlank @Size(min = 6, max = 100) String senha
    ) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String senha) {
    }

    public record UsuarioView(UUID id, String nome, String email) {
        static UsuarioView from(Usuario usuario) {
            return new UsuarioView(usuario.getId(), usuario.getNome(), usuario.getEmail());
        }
    }

    public record SessaoResponse(UsuarioView usuario, String token) {
        static SessaoResponse from(Sessao sessao) {
            return new SessaoResponse(UsuarioView.from(sessao.usuario()), sessao.token());
        }
    }

    private final AuthService authService;
    private final AuthTokens authTokens;

    public AuthController(AuthService authService, AuthTokens authTokens) {
        this.authService = authService;
        this.authTokens = authTokens;
    }

    @PostMapping("/register")
    public ResponseEntity<SessaoResponse> registrar(@Valid @RequestBody RegistrarRequest request) {
        return ResponseEntity.status(201)
                .body(SessaoResponse.from(authService.registrar(request.nome(), request.email(), request.senha())));
    }

    @PostMapping("/login")
    public SessaoResponse login(@Valid @RequestBody LoginRequest request) {
        return SessaoResponse.from(authService.login(request.email(), request.senha()));
    }

    @GetMapping("/me")
    public UsuarioView me(HttpServletRequest request) {
        return UsuarioView.from(authService.buscar(authTokens.exigirUsuario(request)));
    }
}
