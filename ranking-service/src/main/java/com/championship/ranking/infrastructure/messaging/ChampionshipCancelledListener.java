package com.championship.ranking.infrastructure.messaging;

import com.championship.ranking.infrastructure.messaging.events.ChampionshipCancelledPayload;
import com.championship.ranking.infrastructure.messaging.events.DomainEventEnvelope;
import com.championship.ranking.infrastructure.persistence.GroupStandingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ChampionshipCancelledListener {

    private static final Logger log = LoggerFactory.getLogger(ChampionshipCancelledListener.class);

    private final ObjectMapper objectMapper;
    private final GroupStandingRepository groupStandingRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final TraceIdProvider traceIdProvider;

    public ChampionshipCancelledListener(ObjectMapper objectMapper,
                                          GroupStandingRepository groupStandingRepository,
                                          ProcessedEventRepository processedEventRepository,
                                          TraceIdProvider traceIdProvider) {
        this.objectMapper = objectMapper;
        this.groupStandingRepository = groupStandingRepository;
        this.processedEventRepository = processedEventRepository;
        this.traceIdProvider = traceIdProvider;
    }

    @KafkaListener(topics = ChampionshipCancelledPayload.TYPE)
    @Transactional
    public void onChampionshipCancelled(String message) throws Exception {
        DomainEventEnvelope<ChampionshipCancelledPayload> envelope = objectMapper.readValue(
                message, objectMapper.getTypeFactory().constructParametricType(
                        DomainEventEnvelope.class, ChampionshipCancelledPayload.class));

        if (processedEventRepository.existsById(envelope.eventId())) {
            log.info("event already processed, skipping eventId={}", envelope.eventId());
            return;
        }

        UUID championshipId = envelope.payload().championshipId();
        groupStandingRepository.deleteByCampeonatoId(championshipId);

        processedEventRepository.save(new ProcessedEvent(
                envelope.eventId(), envelope.type(), envelope.aggregateId(),
                message, traceIdProvider.currentTraceId()));
        log.info("torneio cancelado — classificacao projetada purgada campeonatoId={}", championshipId);
    }
}
