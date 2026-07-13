package com.championship.ranking.infrastructure.messaging;

import com.championship.ranking.application.RankingProjectionService;
import com.championship.ranking.infrastructure.messaging.events.DomainEventEnvelope;
import com.championship.ranking.infrastructure.messaging.events.MatchFinishedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer de match.finished.v1 (SPEC.md §3): recalcula a classificação do
 * grupo no read model. Idempotente via processed_events — reentrega da mesma
 * mensagem não conta o resultado duas vezes (skill kafka-event-design).
 */
@Component
public class MatchFinishedListener {

    private static final Logger log = LoggerFactory.getLogger(MatchFinishedListener.class);

    private final ObjectMapper objectMapper;
    private final RankingProjectionService rankingProjectionService;
    private final ProcessedEventRepository processedEventRepository;
    private final TraceIdProvider traceIdProvider;

    public MatchFinishedListener(ObjectMapper objectMapper,
                                  RankingProjectionService rankingProjectionService,
                                  ProcessedEventRepository processedEventRepository,
                                  TraceIdProvider traceIdProvider) {
        this.objectMapper = objectMapper;
        this.rankingProjectionService = rankingProjectionService;
        this.processedEventRepository = processedEventRepository;
        this.traceIdProvider = traceIdProvider;
    }

    @KafkaListener(topics = MatchFinishedPayload.TYPE)
    @Transactional
    public void onMatchFinished(String message) throws Exception {
        DomainEventEnvelope<MatchFinishedPayload> envelope = objectMapper.readValue(
                message, objectMapper.getTypeFactory().constructParametricType(DomainEventEnvelope.class, MatchFinishedPayload.class));

        if (processedEventRepository.existsById(envelope.eventId())) {
            log.info("event already processed, skipping eventId={}", envelope.eventId());
            return;
        }

        rankingProjectionService.aplicarResultado(envelope.payload());

        // guarda a mensagem bruta e o trace id para rastreabilidade no Monitoramento
        processedEventRepository.save(new ProcessedEvent(
                envelope.eventId(), envelope.type(), envelope.aggregateId(),
                message, traceIdProvider.currentTraceId()));
        log.info("ranking projection updated matchId={} groupId={}",
                envelope.payload().matchId(), envelope.payload().groupId());
    }
}
