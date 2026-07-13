package com.championship.ranking.application;

import com.championship.ranking.application.EventFeedService.FeedEntry;
import com.championship.ranking.application.EventFeedService.Kind;
import com.championship.ranking.infrastructure.messaging.OutboxEvent;
import com.championship.ranking.infrastructure.messaging.OutboxEventRepository;
import com.championship.ranking.infrastructure.messaging.ProcessedEvent;
import com.championship.ranking.infrastructure.messaging.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventFeedServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private EventFeedService eventFeedService;

    @Test
    void juntaConsumidosEPublicadosOrdenadosDoMaisRecente() {
        UUID matchId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID consumidoId = UUID.randomUUID();
        UUID publicadoId = UUID.randomUUID();

        ProcessedEvent consumido = new ProcessedEvent(
                consumidoId, "match.finished.v1", matchId, "{\"home_score\":2}", "abc123trace");
        OutboxEvent publicado = new OutboxEvent(
                publicadoId, groupId, "ranking.updated.v1", "{}",
                Instant.now().plusSeconds(1), "abc123trace");

        when(processedEventRepository.findByEventTypeNotNullOrderByProcessedAtDesc(any()))
                .thenReturn(List.of(consumido));
        when(outboxEventRepository.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(publicado));

        List<FeedEntry> feed = eventFeedService.recentes(10);

        assertThat(feed).hasSize(2);
        // publicado tem timestamp mais recente -> vem primeiro
        assertThat(feed.get(0).kind()).isEqualTo(Kind.PUBLISHED);
        assertThat(feed.get(0).type()).isEqualTo("ranking.updated.v1");
        assertThat(feed.get(0).eventId()).isEqualTo(publicadoId);
        assertThat(feed.get(0).traceId()).isEqualTo("abc123trace");
        assertThat(feed.get(1).kind()).isEqualTo(Kind.CONSUMED);
        assertThat(feed.get(1).type()).isEqualTo("match.finished.v1");
        assertThat(feed.get(1).aggregateId()).isEqualTo(matchId);
        assertThat(feed.get(1).eventId()).isEqualTo(consumidoId);
        assertThat(feed.get(1).payload()).isEqualTo("{\"home_score\":2}");
    }

    @Test
    void respeitaOLimite() {
        UUID id = UUID.randomUUID();
        when(processedEventRepository.findByEventTypeNotNullOrderByProcessedAtDesc(any()))
                .thenReturn(List.of(
                        new ProcessedEvent(UUID.randomUUID(), "match.finished.v1", id),
                        new ProcessedEvent(UUID.randomUUID(), "match.finished.v1", id)));
        when(outboxEventRepository.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(new OutboxEvent(UUID.randomUUID(), id, "ranking.updated.v1", "{}", Instant.now())));

        assertThat(eventFeedService.recentes(2)).hasSize(2);
    }
}
