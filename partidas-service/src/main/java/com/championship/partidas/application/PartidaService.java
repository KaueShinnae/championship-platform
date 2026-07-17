package com.championship.partidas.application;

import com.championship.partidas.domain.Partida;
import com.championship.partidas.infrastructure.messaging.DomainEventWriter;
import com.championship.partidas.infrastructure.messaging.events.MatchFinishedPayload;
import com.championship.partidas.infrastructure.messaging.events.MatchScheduledPayload;
import com.championship.partidas.infrastructure.messaging.events.MatchStartedPayload;
import com.championship.partidas.infrastructure.messaging.events.TeamRef;
import com.championship.partidas.infrastructure.messaging.events.TeamScore;
import com.championship.partidas.infrastructure.persistence.PartidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PartidaService {

    private final PartidaRepository partidaRepository;
    private final DomainEventWriter domainEventWriter;
    private final ChaveamentoService chaveamentoService;

    public PartidaService(PartidaRepository partidaRepository, DomainEventWriter domainEventWriter,
                           ChaveamentoService chaveamentoService) {
        this.partidaRepository = partidaRepository;
        this.domainEventWriter = domainEventWriter;
        this.chaveamentoService = chaveamentoService;
    }

    /**
     * Agenda uma partida e grava match.scheduled.v1 no outbox na mesma transação.
     */
    @Transactional
    public Partida agendar(UUID campeonatoId, UUID groupId,
                            UUID homeTeamId, String homeTeamName,
                            UUID awayTeamId, String awayTeamName,
                            Instant scheduledAt) {
        Partida partida = partidaRepository.save(
                Partida.agendar(campeonatoId, groupId, homeTeamId, homeTeamName, awayTeamId, awayTeamName, scheduledAt));

        domainEventWriter.write(partida.getId(), MatchScheduledPayload.TYPE, new MatchScheduledPayload(
                partida.getId(), partida.getCampeonatoId(), partida.getGroupId(),
                new TeamRef(partida.getHomeTeamId(), partida.getHomeTeamName()),
                new TeamRef(partida.getAwayTeamId(), partida.getAwayTeamName()),
                partida.getScheduledAt()));

        return partida;
    }

    /**
     * Remarca data/horário de uma partida AGENDADA e reemite match.scheduled.v1
     * com o novo horário (mesmo contrato do agendamento; consumers são idempotentes).
     */
    @Transactional
    public Partida reagendar(UUID partidaId, Instant novoHorario) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new IllegalArgumentException("partida nao encontrada: " + partidaId));

        partida.reagendar(novoHorario);
        partidaRepository.save(partida);

        domainEventWriter.write(partida.getId(), MatchScheduledPayload.TYPE, new MatchScheduledPayload(
                partida.getId(), partida.getCampeonatoId(), partida.getGroupId(),
                new TeamRef(partida.getHomeTeamId(), partida.getHomeTeamName()),
                new TeamRef(partida.getAwayTeamId(), partida.getAwayTeamName()),
                partida.getScheduledAt()));

        return partida;
    }

    /**
     * Inicia a partida (AGENDADA -> EM_ANDAMENTO) e grava match.started.v1
     * no outbox na mesma transação.
     */
    @Transactional
    public Partida iniciar(UUID partidaId) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new IllegalArgumentException("partida nao encontrada: " + partidaId));

        partida.iniciar();
        partidaRepository.save(partida);

        domainEventWriter.write(partida.getId(), MatchStartedPayload.TYPE, new MatchStartedPayload(
                partida.getId(), partida.getCampeonatoId(), partida.getGroupId(), partida.getStartedAt()));

        return partida;
    }

    /**
     * Placar parcial (contagem ao vivo): estado operacional lido via REST/polling.
     * Sem evento de domínio — o evento continua sendo só o match.finished.v1
     * (eventos pequenos e específicos; o produto é gestão, não transmissão).
     */
    @Transactional
    public Partida atualizarPlacar(UUID partidaId, int homeScore, int awayScore) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new IllegalArgumentException("partida nao encontrada: " + partidaId));

        partida.atualizarPlacar(homeScore, awayScore);
        return partidaRepository.save(partida);
    }

    /**
     * Registra o resultado e grava match.finished.v1 no outbox na mesma
     * transação — dispara o recálculo de ranking (sem chamada síncrona).
     */
    @Transactional
    public Partida registrarResultado(UUID partidaId, int homeScore, int awayScore) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new IllegalArgumentException("partida nao encontrada: " + partidaId));

        partida.registrarResultado(homeScore, awayScore);
        partidaRepository.save(partida);

        domainEventWriter.write(partida.getId(), MatchFinishedPayload.TYPE, new MatchFinishedPayload(
                partida.getId(), partida.getCampeonatoId(), partida.getGroupId(),
                new TeamScore(partida.getHomeTeamId(), partida.getHomeTeamName(), partida.getHomeScore()),
                new TeamScore(partida.getAwayTeamId(), partida.getAwayTeamName(), partida.getAwayScore()),
                partida.getPlayedAt()));

        // avanço automático de fase (mesma transação): vencedor ocupa o slot
        // seguinte do bracket, grupos completos semeiam os playoffs, e o
        // campeão publica championship.completed.v1
        chaveamentoService.aoFinalizar(partida);

        return partida;
    }

    @Transactional(readOnly = true)
    public Partida buscar(UUID partidaId) {
        return partidaRepository.findById(partidaId)
                .orElseThrow(() -> new IllegalArgumentException("partida nao encontrada: " + partidaId));
    }

    @Transactional(readOnly = true)
    public List<Partida> listar(UUID groupId) {
        return groupId != null
                ? partidaRepository.findByGroupIdOrderByScheduledAtDesc(groupId)
                : partidaRepository.findAllByOrderByScheduledAtDesc();
    }
}
