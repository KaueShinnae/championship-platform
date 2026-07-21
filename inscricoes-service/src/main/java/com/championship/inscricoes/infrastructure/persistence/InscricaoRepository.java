package com.championship.inscricoes.infrastructure.persistence;

import com.championship.inscricoes.domain.Inscricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InscricaoRepository extends JpaRepository<Inscricao, UUID> {

    Optional<Inscricao> findByTimeIdAndCampeonatoId(UUID timeId, UUID campeonatoId);

    @Query("""
            select count(i) > 0 from Inscricao i
            where i.campeonato.id = :campeonatoId
              and lower(i.time.nome) = lower(:nomeTime)
              and i.status <> com.championship.inscricoes.domain.InscricaoStatus.RECUSADA
            """)
    boolean existsAtivaByCampeonatoIdAndNomeTime(UUID campeonatoId, String nomeTime);

    @Query("""
            select count(i) > 0 from Inscricao i
            where i.campeonato.id = :campeonatoId
              and i.capitaoUsuarioId = :capitaoUsuarioId
              and i.status = com.championship.inscricoes.domain.InscricaoStatus.PENDENTE
            """)
    boolean existsPendenteDoCapitao(UUID campeonatoId, UUID capitaoUsuarioId);

    @Query("""
            select distinct i from Inscricao i
            join fetch i.time t
            left join fetch t.jogadores
            where i.campeonato.id = :campeonatoId
            order by i.createdAt asc
            """)
    List<Inscricao> findDetalhadoByCampeonatoId(UUID campeonatoId);
}
