package com.championship.inscricoes.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Ciclo de vida do torneio (ver especificação de formatos):
 * ABERTO -(sortear)-> SORTEADO -(iniciar)-> EM_ANDAMENTO -(campeão)-> ENCERRADO.
 * Inscrições só são aceitas em ABERTO; o sorteio pode ser refeito ou
 * descartado (reabrir) enquanto o torneio não iniciar.
 */
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampeonatoFormato formato;

    @Column(name = "campeao_time_id")
    private UUID campeaoTimeId;

    @Column(name = "campeao_nome", length = 100)
    private String campeaoNome;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Campeonato() {
    }

    private Campeonato(UUID id, String nome, CampeonatoFormato formato, CampeonatoStatus status, Instant createdAt) {
        this.id = id;
        this.nome = nome;
        this.formato = formato;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Campeonato criar(String nome, CampeonatoFormato formato) {
        if (nome == null || nome.isBlank() || nome.length() > 100) {
            throw new IllegalArgumentException("nome do campeonato deve ter entre 1 e 100 caracteres");
        }
        if (formato == null) {
            throw new IllegalArgumentException("formato do campeonato e obrigatorio");
        }
        return new Campeonato(UUID.randomUUID(), nome, formato, CampeonatoStatus.ABERTO, Instant.now());
    }

    /** Compatibilidade com o fluxo antigo: sem formato explícito, pontos corridos. */
    public static Campeonato criar(String nome) {
        return criar(nome, CampeonatoFormato.PONTOS_CORRIDOS);
    }

    public boolean aceitaInscricoes() {
        return status == CampeonatoStatus.ABERTO;
    }

    /** ABERTO -> SORTEADO; re-sortear em SORTEADO é permitido (no-op de status). */
    public void sortear() {
        if (status != CampeonatoStatus.ABERTO && status != CampeonatoStatus.SORTEADO) {
            throw new IllegalStateException("sorteio so e permitido com o campeonato ABERTO ou SORTEADO: " + status);
        }
        this.status = CampeonatoStatus.SORTEADO;
    }

    /** SORTEADO -> ABERTO: descarta o sorteio e reabre as inscrições. */
    public void reabrirInscricoes() {
        if (status != CampeonatoStatus.SORTEADO) {
            throw new IllegalStateException("so um campeonato SORTEADO pode reabrir inscricoes: " + status);
        }
        this.status = CampeonatoStatus.ABERTO;
    }

    /** SORTEADO -> EM_ANDAMENTO: trava inscrições e chaveamento. */
    public void iniciar() {
        if (status != CampeonatoStatus.SORTEADO) {
            throw new IllegalStateException("so um campeonato SORTEADO pode ser iniciado: " + status);
        }
        this.status = CampeonatoStatus.EM_ANDAMENTO;
    }

    /** EM_ANDAMENTO -> ENCERRADO, registrando o campeão (championship.completed.v1). */
    public void encerrar(UUID campeaoTimeId, String campeaoNome) {
        if (status != CampeonatoStatus.EM_ANDAMENTO) {
            throw new IllegalStateException("so um campeonato EM_ANDAMENTO pode ser encerrado: " + status);
        }
        this.status = CampeonatoStatus.ENCERRADO;
        this.campeaoTimeId = campeaoTimeId;
        this.campeaoNome = campeaoNome;
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

    public CampeonatoFormato getFormato() {
        return formato;
    }

    public UUID getCampeaoTimeId() {
        return campeaoTimeId;
    }

    public String getCampeaoNome() {
        return campeaoNome;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
