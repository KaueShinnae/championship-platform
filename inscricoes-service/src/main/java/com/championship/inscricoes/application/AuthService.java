package com.championship.inscricoes.application;

import com.championship.inscricoes.domain.Usuario;
import com.championship.inscricoes.infrastructure.persistence.UsuarioRepository;
import com.championship.inscricoes.infrastructure.security.AuthTokens;
import com.championship.inscricoes.infrastructure.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    public record Sessao(Usuario usuario, String token) {
    }

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;
    private final AuthTokens authTokens;

    public AuthService(UsuarioRepository usuarioRepository, PasswordHasher passwordHasher, AuthTokens authTokens) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher = passwordHasher;
        this.authTokens = authTokens;
    }

    @Transactional
    public Sessao registrar(String nome, String email, String senha) {
        if (senha == null || senha.length() < 6) {
            throw new IllegalArgumentException("senha deve ter pelo menos 6 caracteres");
        }
        Usuario usuario = Usuario.criar(nome, email, passwordHasher.hash(senha));
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalStateException("ja existe uma conta com este email");
        }
        usuarioRepository.save(usuario);
        return new Sessao(usuario, authTokens.emitir(usuario.getId(), usuario.getNome()));
    }

    @Transactional(readOnly = true)
    public Sessao login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email == null ? "" : email.trim().toLowerCase())
                .orElseThrow(() -> new NaoAutenticadoException("email ou senha incorretos"));
        if (!passwordHasher.verificar(senha, usuario.getSenhaHash())) {
            throw new NaoAutenticadoException("email ou senha incorretos");
        }
        return new Sessao(usuario, authTokens.emitir(usuario.getId(), usuario.getNome()));
    }

    @Transactional(readOnly = true)
    public Usuario buscar(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NaoAutenticadoException("conta nao encontrada — entre novamente"));
    }
}
