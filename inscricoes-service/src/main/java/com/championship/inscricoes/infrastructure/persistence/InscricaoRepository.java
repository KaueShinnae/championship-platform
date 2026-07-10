package com.championship.inscricoes.infrastructure.persistence;

import com.championship.inscricoes.domain.Inscricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InscricaoRepository extends JpaRepository<Inscricao, UUID> {

    Optional<Inscricao> findByTimeIdAndCampeonatoId(UUID timeId, UUID campeonatoId);

    /**
     * Unicidade de time por campeonato (SPEC.md §2): nome comparado sem
     * diferenciar maiúsculas para bloquear reinscrição acidental.
     */
    @Query("""
            select count(i) > 0 from Inscricao i
            where i.campeonato.id = :campeonatoId
              and lower(i.time.nome) = lower(:nomeTime)
            """)
    boolean existsByCampeonatoIdAndNomeTime(UUID campeonatoId, String nomeTime);

    /**
     * Fetch-join de time e jogadores: a listagem é consumida fora da
     * transação (open-in-view desligado), então tudo vem carregado.
     */
    @Query("""
            select distinct i from Inscricao i
            join fetch i.time t
            left join fetch t.jogadores
            where i.campeonato.id = :campeonatoId
            order by i.createdAt asc
            """)
    List<Inscricao> findDetalhadoByCampeonatoId(UUID campeonatoId);
}
