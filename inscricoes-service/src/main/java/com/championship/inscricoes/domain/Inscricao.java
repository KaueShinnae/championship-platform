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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected Inscricao() {
    }

    private Inscricao(UUID id, Time time, Campeonato campeonato, InscricaoStatus status, Instant createdAt) {
        this.id = id;
        this.time = time;
        this.campeonato = campeonato;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Inscricao pendente(Time time, Campeonato campeonato) {
        return new Inscricao(UUID.randomUUID(), time, campeonato, InscricaoStatus.PENDENTE, Instant.now());
    }

    public void confirmar() {
        if (this.status == InscricaoStatus.CONFIRMADA) {
            return;
        }
        this.status = InscricaoStatus.CONFIRMADA;
        this.confirmedAt = Instant.now();
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }
}
