package com.championship.inscricoes.api;

import com.championship.inscricoes.domain.Inscricao;
import com.championship.inscricoes.domain.InscricaoStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TimeDtos {

    public record InscreverTimeRequest(
            @NotBlank @Size(max = 100) String nome,
            @NotEmpty @Valid List<@NotBlank @Size(max = 100) String> jogadores
    ) {
    }

    public record JogadorView(UUID id, String nome) {
    }

    /**
     * Item da listagem do organizador: time inscrito + jogadores + status da
     * saga (PENDENTE ate enrollment.confirmed.v1 ser processado).
     */
    public record InscricaoDetalheResponse(
            UUID inscricaoId,
            UUID timeId,
            String timeNome,
            List<JogadorView> jogadores,
            InscricaoStatus status,
            Instant confirmedAt
    ) {
        public static InscricaoDetalheResponse from(Inscricao inscricao) {
            return new InscricaoDetalheResponse(
                    inscricao.getId(),
                    inscricao.getTime().getId(),
                    inscricao.getTime().getNome(),
                    inscricao.getTime().getJogadores().stream()
                            .map(jogador -> new JogadorView(jogador.getId(), jogador.getNome()))
                            .toList(),
                    inscricao.getStatus(),
                    inscricao.getConfirmedAt());
        }
    }

    public record InscricaoResponse(
            UUID inscricaoId,
            UUID timeId,
            String timeNome,
            UUID campeonatoId,
            InscricaoStatus status,
            Instant createdAt
    ) {
        public static InscricaoResponse from(Inscricao inscricao) {
            return new InscricaoResponse(
                    inscricao.getId(),
                    inscricao.getTime().getId(),
                    inscricao.getTime().getNome(),
                    inscricao.getCampeonato().getId(),
                    inscricao.getStatus(),
                    inscricao.getCreatedAt());
        }
    }

    private TimeDtos() {
    }
}
