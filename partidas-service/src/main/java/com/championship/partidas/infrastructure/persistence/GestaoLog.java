package com.championship.partidas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gestao_log")
public class GestaoLog {

    @Id
    private UUID id;

    @Column(name = "campeonato_id", nullable = false)
    private UUID campeonatoId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "actor_nome", nullable = false, length = 100)
    private String actorNome;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(nullable = false, length = 300)
    private String descricao;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GestaoLog() {
    }

    public GestaoLog(UUID campeonatoId, UUID actorId, String actorNome, String acao, String descricao) {
        this.id = UUID.randomUUID();
        this.campeonatoId = campeonatoId;
        this.actorId = actorId;
        this.actorNome = actorNome;
        this.acao = acao;
        this.descricao = descricao;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCampeonatoId() {
        return campeonatoId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorNome() {
        return actorNome;
    }

    public String getAcao() {
        return acao;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
