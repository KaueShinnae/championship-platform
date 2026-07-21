package com.championship.ranking.application;

import com.championship.ranking.infrastructure.messaging.OutboxEventRepository;
import com.championship.ranking.infrastructure.messaging.ProcessedEventRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class EventFeedService {

    public enum Kind { CONSUMED, PUBLISHED }

    public record FeedEntry(Kind kind, String type, UUID eventId, UUID aggregateId,
                            String traceId, String payload, Instant at) {
    }

    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;

    public EventFeedService(ProcessedEventRepository processedEventRepository,
                             OutboxEventRepository outboxEventRepository) {
        this.processedEventRepository = processedEventRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(readOnly = true)
    public List<FeedEntry> recentes(int limit) {
        List<FeedEntry> entries = new ArrayList<>();

        processedEventRepository.findByEventTypeNotNullOrderByProcessedAtDesc(Limit.of(limit))
                .forEach(event -> entries.add(new FeedEntry(
                        Kind.CONSUMED, event.getEventType(), event.getEventId(), event.getAggregateId(),
                        event.getTraceId(), event.getPayload(), event.getProcessedAt())));

        outboxEventRepository.findAllByOrderByCreatedAtDesc(Limit.of(limit))
                .forEach(event -> entries.add(new FeedEntry(
                        Kind.PUBLISHED, event.getType(), event.getId(), event.getAggregateId(),
                        event.getTraceId(), event.getPayload(), event.getOccurredAt())));

        return entries.stream()
                .sorted(Comparator.comparing(FeedEntry::at).reversed())
                .limit(limit)
                .toList();
    }
}
