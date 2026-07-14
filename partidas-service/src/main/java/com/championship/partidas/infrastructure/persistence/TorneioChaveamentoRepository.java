package com.championship.partidas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TorneioChaveamentoRepository extends JpaRepository<TorneioChaveamento, UUID> {
}
