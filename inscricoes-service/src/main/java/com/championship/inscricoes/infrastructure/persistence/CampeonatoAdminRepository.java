package com.championship.inscricoes.infrastructure.persistence;

import com.championship.inscricoes.domain.CampeonatoAdmin;
import com.championship.inscricoes.domain.CampeonatoAdmin.CampeonatoAdminId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampeonatoAdminRepository extends JpaRepository<CampeonatoAdmin, CampeonatoAdminId> {

    List<CampeonatoAdmin> findByIdCampeonatoId(UUID campeonatoId);

    List<CampeonatoAdmin> findByIdUsuarioId(UUID usuarioId);
}
