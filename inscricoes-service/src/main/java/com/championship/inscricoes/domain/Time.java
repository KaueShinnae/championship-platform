package com.championship.inscricoes.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "time")
public class Time {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @OneToMany(mappedBy = "time", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Jogador> jogadores = new ArrayList<>();

    @Column(name = "criado_por")
    private UUID criadoPor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Time() {
    }

    private Time(UUID id, String nome, UUID criadoPor, Instant createdAt) {
        this.id = id;
        this.nome = nome;
        this.criadoPor = criadoPor;
        this.createdAt = createdAt;
    }

    public static Time criar(String nome, List<String> nomesJogadores) {
        return criar(nome, nomesJogadores, null);
    }

    public static Time criar(String nome, List<String> nomesJogadores, UUID criadoPor) {
        if (nome == null || nome.isBlank() || nome.length() > 100) {
            throw new IllegalArgumentException("nome do time deve ter entre 1 e 100 caracteres");
        }
        if (nomesJogadores == null || nomesJogadores.isEmpty()) {
            throw new IllegalArgumentException("time precisa de pelo menos 1 jogador");
        }
        Time time = new Time(UUID.randomUUID(), nome, criadoPor, Instant.now());
        nomesJogadores.forEach(nomeJogador -> time.jogadores.add(new Jogador(time, nomeJogador)));
        return time;
    }

    public void editar(String novoNome, List<String> nomesJogadores) {
        if (novoNome == null || novoNome.isBlank() || novoNome.length() > 100) {
            throw new IllegalArgumentException("nome do time deve ter entre 1 e 100 caracteres");
        }
        if (nomesJogadores == null || nomesJogadores.isEmpty()) {
            throw new IllegalArgumentException("time precisa de pelo menos 1 jogador");
        }
        this.nome = novoNome;
        this.jogadores.clear();
        nomesJogadores.forEach(nomeJogador -> this.jogadores.add(new Jogador(this, nomeJogador)));
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public List<Jogador> getJogadores() {
        return List.copyOf(jogadores);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
