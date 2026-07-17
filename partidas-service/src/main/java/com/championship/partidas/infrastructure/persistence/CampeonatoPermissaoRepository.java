package com.championship.partidas.infrastructure.persistence;

import com.championship.partidas.infrastructure.persistence.CampeonatoPermissao.CampeonatoPermissaoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.UUID;

public interface CampeonatoPermissaoRepository extends JpaRepository<CampeonatoPermissao, CampeonatoPermissaoId> {

    boolean existsByIdCampeonatoId(UUID campeonatoId);

    @Modifying
    void deleteByIdCampeonatoId(UUID campeonatoId);
}
