package com.championship.inscricoes.infrastructure.persistence;

import com.championship.inscricoes.domain.Time;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TimeRepository extends JpaRepository<Time, UUID> {
}
