package com.championship.ranking.infrastructure.messaging;

import com.championship.ranking.application.RankingProjectionService;
import com.championship.ranking.infrastructure.messaging.events.DomainEventEnvelope;
import com.championship.ranking.infrastructure.messaging.events.MatchResultCorrectedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MatchResultCorrectedListener {

    private static final Logger log = LoggerFactory.getLogger(MatchResultCorrectedListener.class);

    private final ObjectMapper objectMapper;
    private final RankingProjectionService rankingProjectionService;
    private final ProcessedEventRepository processedEventRepository;
    private final TraceIdProvider traceIdProvider;

    public MatchResultCorrectedListener(ObjectMapper objectMapper,
                                         RankingProjectionService rankingProjectionService,
                                         ProcessedEventRepository processedEventRepository,
                                         TraceIdProvider traceIdProvider) {
        this.objectMapper = objectMapper;
        this.rankingProjectionService = rankingProjectionService;
        this.processedEventRepository = processedEventRepository;
        this.traceIdProvider = traceIdProvider;
    }

    @KafkaListener(topics = MatchResultCorrectedPayload.TYPE)
    @Transactional
    public void onMatchResultCorrected(String message) throws Exception {
        DomainEventEnvelope<MatchResultCorrectedPayload> envelope = objectMapper.readValue(
                message, objectMapper.getTypeFactory().constructParametricType(
                        DomainEventEnvelope.class, MatchResultCorrectedPayload.class));

        if (processedEventRepository.existsById(envelope.eventId())) {
            log.info("event already processed, skipping eventId={}", envelope.eventId());
            return;
        }

        rankingProjectionService.aplicarCorrecao(envelope.payload());

        processedEventRepository.save(new ProcessedEvent(
                envelope.eventId(), envelope.type(), envelope.aggregateId(),
                message, traceIdProvider.currentTraceId()));
        log.info("ranking corrigido matchId={} groupId={}",
                envelope.payload().matchId(), envelope.payload().groupId());
    }
}
