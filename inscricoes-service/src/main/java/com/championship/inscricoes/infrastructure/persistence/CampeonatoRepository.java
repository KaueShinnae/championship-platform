package com.championship.inscricoes.infrastructure.persistence;

import com.championship.inscricoes.domain.Campeonato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CampeonatoRepository extends JpaRepository<Campeonato, UUID> {
}
