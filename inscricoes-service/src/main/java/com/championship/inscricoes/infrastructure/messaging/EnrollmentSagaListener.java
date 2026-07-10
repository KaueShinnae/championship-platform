package com.championship.inscricoes.infrastructure.messaging;

import com.championship.inscricoes.domain.Inscricao;
import com.championship.inscricoes.infrastructure.messaging.events.DomainEventEnvelope;
import com.championship.inscricoes.infrastructure.messaging.events.EnrollmentConfirmedPayload;
import com.championship.inscricoes.infrastructure.messaging.events.TeamRegisteredPayload;
import com.championship.inscricoes.infrastructure.persistence.InscricaoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Próximo passo da saga coreografada de inscrição (ver SPEC.md §3):
 * ao consumir team.registered.v1, confirma a inscrição pendente e publica
 * enrollment.confirmed.v1. Sem orquestrador central — cada serviço reage
 * ao evento anterior e decide o próprio próximo passo.
 */
@Component
public class EnrollmentSagaListener {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentSagaListener.class);

    private final ObjectMapper objectMapper;
    private final InscricaoRepository inscricaoRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final DomainEventWriter domainEventWriter;

    public EnrollmentSagaListener(ObjectMapper objectMapper,
                                   InscricaoRepository inscricaoRepository,
                                   ProcessedEventRepository processedEventRepository,
                                   DomainEventWriter domainEventWriter) {
        this.objectMapper = objectMapper;
        this.inscricaoRepository = inscricaoRepository;
        this.processedEventRepository = processedEventRepository;
        this.domainEventWriter = domainEventWriter;
    }

    @KafkaListener(topics = TeamRegisteredPayload.TYPE)
    @Transactional
    public void onTeamRegistered(String message) throws Exception {
        DomainEventEnvelope<TeamRegisteredPayload> envelope = objectMapper.readValue(
                message, objectMapper.getTypeFactory().constructParametricType(DomainEventEnvelope.class, TeamRegisteredPayload.class));

        if (processedEventRepository.existsById(envelope.eventId())) {
            log.info("event already processed, skipping eventId={}", envelope.eventId());
            return;
        }

        TeamRegisteredPayload payload = envelope.payload();
        Inscricao inscricao = inscricaoRepository
                .findByTimeIdAndCampeonatoId(payload.teamId(), payload.championshipId())
                .orElseThrow(() -> new IllegalStateException(
                        "inscricao nao encontrada para time=" + payload.teamId() + " campeonato=" + payload.championshipId()));

        inscricao.confirmar();
        inscricaoRepository.save(inscricao);

        domainEventWriter.write(inscricao.getId(), EnrollmentConfirmedPayload.TYPE, new EnrollmentConfirmedPayload(
                inscricao.getId(),
                payload.teamId(),
                payload.championshipId(),
                inscricao.getGroupId(),
                inscricao.getConfirmedAt()
        ));

        processedEventRepository.save(new ProcessedEvent(envelope.eventId()));
        log.info("enrollment confirmed inscricaoId={}", inscricao.getId());
    }
}
