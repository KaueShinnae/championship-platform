package com.championship.inscricoes.infrastructure.persistence;

import com.championship.inscricoes.domain.Time;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TimeRepository extends JpaRepository<Time, UUID> {

    /**
     * Times sugeridos para reuso: os que o usuário cadastrou (como capitão ou
     * organizador) e os inscritos em torneios que ele gerencia. Fetch-join dos
     * jogadores porque o resultado é consumido fora da transação.
     */
    @Query("""
            select distinct t from Time t
            left join fetch t.jogadores
            where t.criadoPor = :usuarioId
               or t.id in (
                   select i.time.id from Inscricao i
                   where i.campeonato.donoId = :usuarioId
                      or exists (
                          select 1 from CampeonatoAdmin a
                          where a.id.campeonatoId = i.campeonato.id
                            and a.id.usuarioId = :usuarioId))
            order by t.createdAt desc
            """)
    List<Time> findReutilizaveisPor(UUID usuarioId);
}
