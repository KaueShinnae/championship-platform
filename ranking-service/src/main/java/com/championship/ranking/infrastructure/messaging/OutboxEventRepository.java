package com.championship.ranking.infrastructure.messaging;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc(Limit limit);

    List<OutboxEvent> findAllByOrderByCreatedAtDesc(Limit limit);
}
