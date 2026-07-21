package com.championship.partidas.infrastructure.messaging;

import com.championship.partidas.infrastructure.messaging.events.ChampionshipPermissionsChangedPayload;
import com.championship.partidas.infrastructure.messaging.events.DomainEventEnvelope;
import com.championship.partidas.infrastructure.persistence.CampeonatoPermissao;
import com.championship.partidas.infrastructure.persistence.CampeonatoPermissaoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ChampionshipPermissionsListener {

    private static final Logger log = LoggerFactory.getLogger(ChampionshipPermissionsListener.class);

    private final ObjectMapper objectMapper;
    private final CampeonatoPermissaoRepository permissaoRepository;
    private final ProcessedEventRepository processedEventRepository;

    public ChampionshipPermissionsListener(ObjectMapper objectMapper,
                                            CampeonatoPermissaoRepository permissaoRepository,
                                            ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.permissaoRepository = permissaoRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = ChampionshipPermissionsChangedPayload.TYPE)
    @Transactional
    public void onPermissionsChanged(String message) throws Exception {
        DomainEventEnvelope<ChampionshipPermissionsChangedPayload> envelope = objectMapper.readValue(
                message, objectMapper.getTypeFactory().constructParametricType(
                        DomainEventEnvelope.class, ChampionshipPermissionsChangedPayload.class));

        if (processedEventRepository.existsById(envelope.eventId())) {
            log.info("event already processed, skipping eventId={}", envelope.eventId());
            return;
        }

        ChampionshipPermissionsChangedPayload payload = envelope.payload();
        permissaoRepository.deleteByIdCampeonatoId(payload.championshipId());
        permissaoRepository.save(new CampeonatoPermissao(payload.championshipId(), payload.ownerId(), "DONO"));
        for (UUID adminId : payload.adminIds()) {
            permissaoRepository.save(new CampeonatoPermissao(payload.championshipId(), adminId, "ADMIN"));
        }

        processedEventRepository.save(new ProcessedEvent(envelope.eventId()));
        log.info("projecao de permissoes atualizada campeonatoId={} admins={}",
                payload.championshipId(), payload.adminIds().size());
    }
}
