package com.championship.inscricoes.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inscricao")
public class Inscricao {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_id", nullable = false)
    private Time time;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campeonato_id", nullable = false)
    private Campeonato campeonato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InscricaoStatus status;

    @Column(name = "group_id")
    private UUID groupId;

    /** Usuário que auto-inscreveu o time; nulo quando o organizador inscreveu direto. */
    @Column(name = "capitao_usuario_id")
    private UUID capitaoUsuarioId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected Inscricao() {
    }

    private Inscricao(UUID id, Time time, Campeonato campeonato, InscricaoStatus status,
                       UUID capitaoUsuarioId, Instant createdAt) {
        this.id = id;
        this.time = time;
        this.campeonato = campeonato;
        this.status = status;
        this.capitaoUsuarioId = capitaoUsuarioId;
        this.createdAt = createdAt;
    }

    public static Inscricao pendente(Time time, Campeonato campeonato) {
        return new Inscricao(UUID.randomUUID(), time, campeonato, InscricaoStatus.PENDENTE, null, Instant.now());
    }

    /** Auto-inscrição pelo capitão: fica PENDENTE até o organizador aprovar. */
    public static Inscricao pendenteDeCapitao(Time time, Campeonato campeonato, UUID capitaoUsuarioId) {
        return new Inscricao(UUID.randomUUID(), time, campeonato, InscricaoStatus.PENDENTE,
                capitaoUsuarioId, Instant.now());
    }

    public void confirmar() {
        if (this.status == InscricaoStatus.CONFIRMADA) {
            return;
        }
        this.status = InscricaoStatus.CONFIRMADA;
        this.confirmedAt = Instant.now();
    }

    /** PENDENTE -> RECUSADA: o capitão pode tentar de novo com outra inscrição. */
    public void recusar() {
        if (this.status != InscricaoStatus.PENDENTE) {
            throw new IllegalStateException("so uma inscricao PENDENTE pode ser recusada: " + this.status);
        }
        this.status = InscricaoStatus.RECUSADA;
    }

    public UUID getId() {
        return id;
    }

    public Time getTime() {
        return time;
    }

    public Campeonato getCampeonato() {
        return campeonato;
    }

    public InscricaoStatus getStatus() {
        return status;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getCapitaoUsuarioId() {
        return capitaoUsuarioId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }
}
