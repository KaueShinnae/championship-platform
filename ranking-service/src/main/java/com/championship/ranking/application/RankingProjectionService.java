package com.championship.ranking.application;

import com.championship.ranking.domain.GroupStanding;
import com.championship.ranking.infrastructure.messaging.DomainEventWriter;
import com.championship.ranking.infrastructure.messaging.events.MatchFinishedPayload;
import com.championship.ranking.infrastructure.messaging.events.RankingUpdatedPayload;
import com.championship.ranking.infrastructure.messaging.events.TeamScore;
import com.championship.ranking.infrastructure.persistence.GroupStandingRepository;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class RankingProjectionService {

    private static final Logger log = LoggerFactory.getLogger(RankingProjectionService.class);

    private final GroupStandingRepository groupStandingRepository;
    private final DomainEventWriter domainEventWriter;
    private final ObservationRegistry observationRegistry;

    public RankingProjectionService(GroupStandingRepository groupStandingRepository,
                                     DomainEventWriter domainEventWriter,
                                     ObservationRegistry observationRegistry) {
        this.groupStandingRepository = groupStandingRepository;
        this.domainEventWriter = domainEventWriter;
        this.observationRegistry = observationRegistry;
    }

    /**
     * Aplica um match.finished.v1 na projeção do grupo e grava
     * ranking.updated.v1 no outbox, tudo na mesma transação.
     * Partidas sem group_id são ignoradas (docs/events/match.finished.v1.md).
     */
    @Transactional
    public void aplicarResultado(MatchFinishedPayload payload) {
        if (payload.groupId() == null) {
            log.info("match.finished sem group_id, ignorando para classificacao matchId={}", payload.matchId());
            return;
        }

        // Span customizado no padrão <servico>.<acao> (skill observability-langfuse)
        Observation.createNotStarted("ranking-service.recalculate_group", observationRegistry)
                .lowCardinalityKeyValue("group_id", payload.groupId().toString())
                .observe(() -> {
                    atualizarStanding(payload, payload.homeTeam(), payload.awayTeam());
                    atualizarStanding(payload, payload.awayTeam(), payload.homeTeam());

                    domainEventWriter.write(payload.groupId(), RankingUpdatedPayload.TYPE,
                            new RankingUpdatedPayload(payload.groupId(), payload.championshipId(), Instant.now()));
                });
    }

    private void atualizarStanding(MatchFinishedPayload payload, TeamScore time, TeamScore adversario) {
        GroupStanding standing = groupStandingRepository
                .findByGroupIdAndTeamId(payload.groupId(), time.teamId())
                .orElseGet(() -> GroupStanding.inicial(
                        payload.championshipId(), payload.groupId(), time.teamId(), time.name()));

        standing.atualizarNomeTime(time.name());
        standing.aplicarResultado(time.score(), adversario.score());
        groupStandingRepository.save(standing);
    }

    @Transactional(readOnly = true)
    public List<GroupStanding> classificacaoDoGrupo(java.util.UUID groupId) {
        return groupStandingRepository.findByGroupIdOrderByPointsDesc(groupId).stream()
                .sorted(Comparator.comparingInt(GroupStanding::getPoints).reversed()
                        .thenComparing(Comparator.comparingInt(GroupStanding::getGoalDifference).reversed())
                        .thenComparing(Comparator.comparingInt(GroupStanding::getGoalsFor).reversed())
                        .thenComparing(GroupStanding::getTeamName))
                .toList();
    }
}
