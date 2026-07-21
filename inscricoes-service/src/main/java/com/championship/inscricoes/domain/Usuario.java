package com.championship.inscricoes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 200)
    private String senhaHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Usuario() {
    }

    private Usuario(UUID id, String nome, String email, String senhaHash, Instant createdAt) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.createdAt = createdAt;
    }

    public static Usuario criar(String nome, String email, String senhaHash) {
        if (nome == null || nome.isBlank() || nome.length() > 100) {
            throw new IllegalArgumentException("nome deve ter entre 1 e 100 caracteres");
        }
        if (email == null || email.isBlank() || !email.contains("@") || email.length() > 150) {
            throw new IllegalArgumentException("email invalido");
        }
        return new Usuario(UUID.randomUUID(), nome.trim(), email.trim().toLowerCase(), senhaHash, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }
}
