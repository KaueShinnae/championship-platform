package com.championship.partidas.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Poller simples do padrão transactional outbox: lê eventos não publicados
 * e envia ao Kafka usando o tópico = tipo do evento (ex: match.finished.v1).
 * Ver skill kafka-event-design.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${championship.outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc(Limit.of(BATCH_SIZE));
        for (OutboxEvent event : pending) {
            kafkaTemplate.send(event.getType(), event.getAggregateId().toString(), event.getPayload());
            event.marcarPublicado();
            log.info("outbox event published type={} aggregateId={}", event.getType(), event.getAggregateId());
        }
    }
}
