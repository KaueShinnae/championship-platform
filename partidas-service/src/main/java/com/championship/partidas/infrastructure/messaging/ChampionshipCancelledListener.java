package com.championship.partidas.infrastructure.messaging;

import com.championship.partidas.infrastructure.messaging.events.ChampionshipCancelledPayload;
import com.championship.partidas.infrastructure.messaging.events.DomainEventEnvelope;
import com.championship.partidas.infrastructure.persistence.ChaveSlotRepository;
import com.championship.partidas.infrastructure.persistence.PartidaRepository;
import com.championship.partidas.infrastructure.persistence.TorneioChaveamentoRepository;
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
    private final PartidaRepository partidaRepository;
    private final ChaveSlotRepository chaveSlotRepository;
    private final TorneioChaveamentoRepository chaveamentoRepository;
    private final ProcessedEventRepository processedEventRepository;

    public ChampionshipCancelledListener(ObjectMapper objectMapper,
                                          PartidaRepository partidaRepository,
                                          ChaveSlotRepository chaveSlotRepository,
                                          TorneioChaveamentoRepository chaveamentoRepository,
                                          ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.partidaRepository = partidaRepository;
        this.chaveSlotRepository = chaveSlotRepository;
        this.chaveamentoRepository = chaveamentoRepository;
        this.processedEventRepository = processedEventRepository;
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
        partidaRepository.deleteAll(partidaRepository.findByCampeonatoId(championshipId));
        chaveSlotRepository.deleteByIdCampeonatoId(championshipId);
        chaveamentoRepository.deleteById(championshipId);

        processedEventRepository.save(new ProcessedEvent(envelope.eventId()));
        log.info("torneio cancelado — partidas e chaveamento purgados campeonatoId={}", championshipId);
    }
}
