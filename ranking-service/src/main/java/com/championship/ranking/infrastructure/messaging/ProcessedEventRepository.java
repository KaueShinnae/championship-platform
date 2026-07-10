package com.championship.ranking.infrastructure.messaging;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    List<ProcessedEvent> findByEventTypeNotNullOrderByProcessedAtDesc(Limit limit);
}
