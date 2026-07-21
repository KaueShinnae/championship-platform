package com.championship.ranking.application;

import com.championship.ranking.domain.GroupStanding;
import com.championship.ranking.infrastructure.messaging.DomainEventWriter;
import com.championship.ranking.infrastructure.messaging.events.MatchFinishedPayload;
import com.championship.ranking.infrastructure.messaging.events.RankingUpdatedPayload;
import com.championship.ranking.infrastructure.messaging.events.TeamScore;
import com.championship.ranking.infrastructure.persistence.GroupStandingRepository;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingProjectionServiceTest {

    @Mock
    private GroupStandingRepository groupStandingRepository;

    @Mock
    private DomainEventWriter domainEventWriter;

    private RankingProjectionService service;

    private final UUID groupId = UUID.randomUUID();
    private final UUID championshipId = UUID.randomUUID();
    private final UUID homeTeamId = UUID.randomUUID();
    private final UUID awayTeamId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RankingProjectionService(groupStandingRepository, domainEventWriter, ObservationRegistry.NOOP);
    }

    private MatchFinishedPayload payload(UUID groupId, int homeScore, int awayScore) {
        return new MatchFinishedPayload(
                UUID.randomUUID(), championshipId, groupId,
                new TeamScore(homeTeamId, "Timaço FC", homeScore),
                new TeamScore(awayTeamId, "Rival FC", awayScore),
                false, Instant.now());
    }

    @Test
    void criaStandingsParaTimesNovosEPublicaRankingUpdated() {
        when(groupStandingRepository.findByGroupIdAndTeamId(eq(groupId), any())).thenReturn(Optional.empty());

        service.aplicarResultado(payload(groupId, 2, 1));

        ArgumentCaptor<GroupStanding> captor = ArgumentCaptor.forClass(GroupStanding.class);
        verify(groupStandingRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        GroupStanding home = captor.getAllValues().stream()
                .filter(s -> s.getTeamId().equals(homeTeamId)).findFirst().orElseThrow();
        GroupStanding away = captor.getAllValues().stream()
                .filter(s -> s.getTeamId().equals(awayTeamId)).findFirst().orElseThrow();

        assertThat(home.getPoints()).isEqualTo(3);
        assertThat(away.getPoints()).isZero();

        verify(domainEventWriter).write(eq(groupId), eq(RankingUpdatedPayload.TYPE), any(RankingUpdatedPayload.class));
    }

    @Test
    void atualizaStandingExistenteAcumulandoPontos() {
        GroupStanding existente = GroupStanding.inicial(championshipId, groupId, homeTeamId, "Timaço FC");
        existente.aplicarResultado(1, 0); // ja tinha uma vitoria
        when(groupStandingRepository.findByGroupIdAndTeamId(groupId, homeTeamId)).thenReturn(Optional.of(existente));
        when(groupStandingRepository.findByGroupIdAndTeamId(groupId, awayTeamId)).thenReturn(Optional.empty());

        service.aplicarResultado(payload(groupId, 1, 1));

        assertThat(existente.getPoints()).isEqualTo(4); // 3 (vitoria anterior) + 1 (empate)
        assertThat(existente.getDraws()).isEqualTo(1);
    }

    @Test
    void ignoraPartidaSemGrupo() {
        service.aplicarResultado(payload(null, 2, 1));

        verify(groupStandingRepository, never()).save(any());
        verify(domainEventWriter, never()).write(any(), any(), any());
    }
}
