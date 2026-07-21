package com.championship.inscricoes.api;

import com.championship.inscricoes.domain.Campeonato;
import com.championship.inscricoes.domain.CampeonatoFormato;
import com.championship.inscricoes.domain.CampeonatoStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class CampeonatoDtos {

    public record CriarCampeonatoRequest(
            @NotBlank @Size(max = 100) String nome,
            @NotNull CampeonatoFormato formato,
            Boolean aprovacaoInscricoes, // null = true (padrão: capitães aguardam aprovação)
            Integer minIntegrantes,      // null = sem limite (mín. de integrantes por equipe)
            Integer maxIntegrantes,      // null = sem limite
            Boolean disputaTerceiro      // disputa de 3º lugar no mata-mata
    ) {
    }

    public record EditarCampeonatoRequest(
            @NotBlank @Size(max = 100) String nome,
            Boolean aprovacaoInscricoes, // null = true
            Integer minIntegrantes,
            Integer maxIntegrantes,
            Boolean disputaTerceiro
    ) {
    }

    public record AdicionarAdminRequest(@NotBlank @Size(max = 150) String email) {
    }

    public record GestaoLogResponse(
            UUID id, UUID actorId, String actorNome, String acao, String descricao, Instant createdAt
    ) {
    }

    public record CampeonatoResponse(
            UUID id,
            String nome,
            CampeonatoStatus status,
            CampeonatoFormato formato,
            String campeaoNome,
            boolean canManage,
            boolean isDono,
            boolean semDono,
            boolean aprovacaoInscricoes,
            Integer minIntegrantes,
            Integer maxIntegrantes,
            boolean disputaTerceiro,
            Instant createdAt
    ) {
        public static CampeonatoResponse from(Campeonato campeonato, boolean canManage, boolean isDono) {
            return new CampeonatoResponse(campeonato.getId(), campeonato.getNome(), campeonato.getStatus(),
                    campeonato.getFormato(), campeonato.getCampeaoNome(), canManage, isDono,
                    campeonato.semDono(), campeonato.exigeAprovacaoDeInscricoes(),
                    campeonato.getMinIntegrantes(), campeonato.getMaxIntegrantes(),
                    campeonato.temDisputaTerceiro(), campeonato.getCreatedAt());
        }
    }

    private CampeonatoDtos() {
    }
}
