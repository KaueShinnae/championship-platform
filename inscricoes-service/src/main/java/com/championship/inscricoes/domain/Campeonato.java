package com.championship.inscricoes.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campeonato")
public class Campeonato {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampeonatoStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Campeonato() {
    }

    private Campeonato(UUID id, String nome, CampeonatoStatus status, Instant createdAt) {
        this.id = id;
        this.nome = nome;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Campeonato criar(String nome) {
        if (nome == null || nome.isBlank() || nome.length() > 100) {
            throw new IllegalArgumentException("nome do campeonato deve ter entre 1 e 100 caracteres");
        }
        return new Campeonato(UUID.randomUUID(), nome, CampeonatoStatus.ABERTO, Instant.now());
    }

    public boolean aceitaInscricoes() {
        return status == CampeonatoStatus.ABERTO;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public CampeonatoStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
