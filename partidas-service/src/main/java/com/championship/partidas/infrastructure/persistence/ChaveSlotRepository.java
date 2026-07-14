package com.championship.partidas.infrastructure.persistence;

import com.championship.partidas.infrastructure.persistence.ChaveSlot.ChaveSlotId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChaveSlotRepository extends JpaRepository<ChaveSlot, ChaveSlotId> {

    void deleteByIdCampeonatoId(UUID campeonatoId);
}
