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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampeonatoFormato formato;

    @Column(name = "campeao_time_id")
    private UUID campeaoTimeId;

    @Column(name = "campeao_nome", length = 100)
    private String campeaoNome;

    @Column(name = "dono_id")
    private UUID donoId;

    @Column(name = "aprovacao_inscricoes", nullable = false)
    private boolean aprovacaoInscricoes = true;

    @Column(name = "min_integrantes")
    private Integer minIntegrantes;

    @Column(name = "max_integrantes")
    private Integer maxIntegrantes;

    @Column(name = "disputa_terceiro", nullable = false)
    private boolean disputaTerceiro = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Campeonato() {
    }

    private Campeonato(UUID id, String nome, CampeonatoFormato formato, CampeonatoStatus status,
                        UUID donoId, boolean aprovacaoInscricoes,
                        Integer minIntegrantes, Integer maxIntegrantes, boolean disputaTerceiro, Instant createdAt) {
        this.id = id;
        this.nome = nome;
        this.formato = formato;
        this.status = status;
        this.donoId = donoId;
        this.aprovacaoInscricoes = aprovacaoInscricoes;
        this.minIntegrantes = minIntegrantes;
        this.maxIntegrantes = maxIntegrantes;
        this.disputaTerceiro = disputaTerceiro;
        this.createdAt = createdAt;
    }

    public static Campeonato criar(String nome, CampeonatoFormato formato, UUID donoId,
                                    boolean aprovacaoInscricoes, Integer minIntegrantes, Integer maxIntegrantes,
                                    boolean disputaTerceiro) {
        if (nome == null || nome.isBlank() || nome.length() > 100) {
            throw new IllegalArgumentException("nome do campeonato deve ter entre 1 e 100 caracteres");
        }
        if (formato == null) {
            throw new IllegalArgumentException("formato do campeonato e obrigatorio");
        }
        if (minIntegrantes != null && minIntegrantes < 1) {
            throw new IllegalArgumentException("mínimo de integrantes deve ser pelo menos 1");
        }
        if (minIntegrantes != null && maxIntegrantes != null && maxIntegrantes < minIntegrantes) {
            throw new IllegalArgumentException("máximo de integrantes não pode ser menor que o mínimo");
        }
        return new Campeonato(UUID.randomUUID(), nome, formato, CampeonatoStatus.ABERTO, donoId,
                aprovacaoInscricoes, minIntegrantes, maxIntegrantes,
                disputaTerceiro && formato != CampeonatoFormato.PONTOS_CORRIDOS, Instant.now());
    }

    public static Campeonato criar(String nome, CampeonatoFormato formato, UUID donoId,
                                    boolean aprovacaoInscricoes, Integer minIntegrantes, Integer maxIntegrantes) {
        return criar(nome, formato, donoId, aprovacaoInscricoes, minIntegrantes, maxIntegrantes, false);
    }

    public static Campeonato criar(String nome, CampeonatoFormato formato, UUID donoId,
                                    boolean aprovacaoInscricoes) {
        return criar(nome, formato, donoId, aprovacaoInscricoes, null, null);
    }

    public static Campeonato criar(String nome, CampeonatoFormato formato, UUID donoId) {
        return criar(nome, formato, donoId, true);
    }

    public static Campeonato criar(String nome, CampeonatoFormato formato) {
        return criar(nome, formato, null);
    }

    public static Campeonato criar(String nome) {
        return criar(nome, CampeonatoFormato.PONTOS_CORRIDOS, null);
    }

    public void editar(String nome, boolean aprovacaoInscricoes,
                        Integer minIntegrantes, Integer maxIntegrantes, boolean disputaTerceiro) {
        if (status != CampeonatoStatus.ABERTO) {
            throw new IllegalStateException("só um campeonato ABERTO pode ser editado: " + status);
        }
        if (nome == null || nome.isBlank() || nome.length() > 100) {
            throw new IllegalArgumentException("nome do campeonato deve ter entre 1 e 100 caracteres");
        }
        if (minIntegrantes != null && minIntegrantes < 1) {
            throw new IllegalArgumentException("mínimo de integrantes deve ser pelo menos 1");
        }
        if (minIntegrantes != null && maxIntegrantes != null && maxIntegrantes < minIntegrantes) {
            throw new IllegalArgumentException("máximo de integrantes não pode ser menor que o mínimo");
        }
        this.nome = nome;
        this.aprovacaoInscricoes = aprovacaoInscricoes;
        this.minIntegrantes = minIntegrantes;
        this.maxIntegrantes = maxIntegrantes;
        this.disputaTerceiro = disputaTerceiro && formato != CampeonatoFormato.PONTOS_CORRIDOS;
    }

    public void cancelar() {
        if (status == CampeonatoStatus.ENCERRADO) {
            throw new IllegalStateException("um torneio já encerrado não pode ser cancelado");
        }
        if (status == CampeonatoStatus.CANCELADO) {
            throw new IllegalStateException("o torneio já está cancelado");
        }
        this.status = CampeonatoStatus.CANCELADO;
    }

    public boolean ehDono(UUID usuarioId) {
        return donoId == null || donoId.equals(usuarioId);
    }

    public boolean semDono() {
        return donoId == null;
    }

    public void validarNumeroDeIntegrantes(int quantidade) {
        if (minIntegrantes != null && quantidade < minIntegrantes) {
            throw new IllegalArgumentException(
                    "este torneio exige no mínimo " + minIntegrantes + " integrante(s) por equipe");
        }
        if (maxIntegrantes != null && quantidade > maxIntegrantes) {
            throw new IllegalArgumentException(
                    "este torneio permite no máximo " + maxIntegrantes + " integrante(s) por equipe");
        }
    }

    public Integer getMinIntegrantes() {
        return minIntegrantes;
    }

    public Integer getMaxIntegrantes() {
        return maxIntegrantes;
    }

    public boolean temDisputaTerceiro() {
        return disputaTerceiro;
    }

    public boolean exigeAprovacaoDeInscricoes() {
        return aprovacaoInscricoes;
    }

    public boolean aceitaInscricoes() {
        return status == CampeonatoStatus.ABERTO;
    }

    public void sortear() {
        if (status != CampeonatoStatus.ABERTO && status != CampeonatoStatus.SORTEADO) {
            throw new IllegalStateException("sorteio so e permitido com o campeonato ABERTO ou SORTEADO: " + status);
        }
        this.status = CampeonatoStatus.SORTEADO;
    }

    public void reabrirInscricoes() {
        if (status != CampeonatoStatus.SORTEADO) {
            throw new IllegalStateException("so um campeonato SORTEADO pode reabrir inscricoes: " + status);
        }
        this.status = CampeonatoStatus.ABERTO;
    }

    public void iniciar() {
        if (status != CampeonatoStatus.SORTEADO) {
            throw new IllegalStateException("so um campeonato SORTEADO pode ser iniciado: " + status);
        }
        this.status = CampeonatoStatus.EM_ANDAMENTO;
    }

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

    public UUID getDonoId() {
        return donoId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
