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
            @NotNull CampeonatoFormato formato
    ) {
    }

    public record CampeonatoResponse(
            UUID id,
            String nome,
            CampeonatoStatus status,
            CampeonatoFormato formato,
            String campeaoNome,
            Instant createdAt
    ) {
        public static CampeonatoResponse from(Campeonato campeonato) {
            return new CampeonatoResponse(campeonato.getId(), campeonato.getNome(), campeonato.getStatus(),
                    campeonato.getFormato(), campeonato.getCampeaoNome(), campeonato.getCreatedAt());
        }
    }

    private CampeonatoDtos() {
    }
}
