package com.championship.partidas.infrastructure.persistence;

import com.championship.partidas.domain.Partida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartidaRepository extends JpaRepository<Partida, UUID> {

    List<Partida> findAllByOrderByScheduledAtDesc();

    List<Partida> findByGroupIdOrderByScheduledAtDesc(UUID groupId);

    List<Partida> findByGroupId(UUID groupId);

    List<Partida> findByCampeonatoId(UUID campeonatoId);
}
