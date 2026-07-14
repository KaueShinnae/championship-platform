package com.championship.inscricoes.infrastructure.messaging;

import com.championship.inscricoes.domain.Campeonato;
import com.championship.inscricoes.infrastructure.messaging.events.ChampionshipCompletedPayload;
import com.championship.inscricoes.infrastructure.messaging.events.DomainEventEnvelope;
import com.championship.inscricoes.infrastructure.persistence.CampeonatoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encerramento automático do torneio: consome championship.completed.v1
 * (publicado pelo partidas-service quando o campeão é definido) e marca o
 * campeonato como ENCERRADO com o campeão registrado. Idempotente via
 * processed_events (skill kafka-event-design).
 */
@Component
public class ChampionshipCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(ChampionshipCompletedListener.class);

    private final ObjectMapper objectMapper;
    private final CampeonatoRepository campeonatoRepository;
    private final ProcessedEventRepository processedEventRepository;

    public ChampionshipCompletedListener(ObjectMapper objectMapper,
                                          CampeonatoRepository campeonatoRepository,
                                          ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.campeonatoRepository = campeonatoRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = ChampionshipCompletedPayload.TYPE)
    @Transactional
    public void onChampionshipCompleted(String message) throws Exception {
        DomainEventEnvelope<ChampionshipCompletedPayload> envelope = objectMapper.readValue(
                message, objectMapper.getTypeFactory().constructParametricType(
                        DomainEventEnvelope.class, ChampionshipCompletedPayload.class));

        if (processedEventRepository.existsById(envelope.eventId())) {
            log.info("event already processed, skipping eventId={}", envelope.eventId());
            return;
        }

        ChampionshipCompletedPayload payload = envelope.payload();
        Campeonato campeonato = campeonatoRepository.findById(payload.championshipId())
                .orElseThrow(() -> new IllegalStateException(
                        "campeonato nao encontrado: " + payload.championshipId()));

        campeonato.encerrar(payload.champion().teamId(), payload.champion().name());
        campeonatoRepository.save(campeonato);

        processedEventRepository.save(new ProcessedEvent(envelope.eventId()));
        log.info("campeonato encerrado campeonatoId={} campeao={}",
                payload.championshipId(), payload.champion().name());
    }
}
