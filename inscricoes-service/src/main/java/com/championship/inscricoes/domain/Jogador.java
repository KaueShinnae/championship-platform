package com.championship.inscricoes.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "jogador")
public class Jogador {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_id", nullable = false)
    private Time time;

    @Column(nullable = false, length = 100)
    private String nome;

    protected Jogador() {
    }

    Jogador(Time time, String nome) {
        if (nome == null || nome.isBlank() || nome.length() > 100) {
            throw new IllegalArgumentException("nome do jogador deve ter entre 1 e 100 caracteres");
        }
        this.id = UUID.randomUUID();
        this.time = time;
        this.nome = nome;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
