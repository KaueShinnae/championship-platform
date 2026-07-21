package com.championship.inscricoes.infrastructure.persistence;

import com.championship.inscricoes.domain.GestaoLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GestaoLogRepository extends JpaRepository<GestaoLog, UUID> {

    List<GestaoLog> findByCampeonatoIdOrderByCreatedAtDesc(UUID campeonatoId);
}
