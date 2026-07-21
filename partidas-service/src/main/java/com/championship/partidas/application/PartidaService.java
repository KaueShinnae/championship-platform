package com.championship.partidas.application;

import com.championship.partidas.domain.Partida;
import com.championship.partidas.domain.PartidaStatus;
import com.championship.partidas.infrastructure.messaging.DomainEventWriter;
import com.championship.partidas.infrastructure.messaging.events.MatchFinishedPayload;
import com.championship.partidas.infrastructure.messaging.events.MatchResultCorrectedPayload;
import com.championship.partidas.infrastructure.messaging.events.MatchScheduledPayload;
import com.championship.partidas.infrastructure.messaging.events.MatchStartedPayload;
import com.championship.partidas.infrastructure.messaging.events.TeamRef;
import com.championship.partidas.infrastructure.messaging.events.TeamScore;
import com.championship.partidas.infrastructure.persistence.GestaoLog;
import com.championship.partidas.infrastructure.persistence.GestaoLogRepository;
import com.championship.partidas.infrastructure.persistence.PartidaRepository;
import com.championship.partidas.infrastructure.security.AuthTokens.Sessao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PartidaService {

    private final PartidaRepository partidaRepository;
    private final DomainEventWriter domainEventWriter;
    private final ChaveamentoService chaveamentoService;
    private final GestaoLogRepository gestaoLogRepository;
    private final StandingsService standingsService;

    public PartidaService(PartidaRepository partidaRepository, DomainEventWriter domainEventWriter,
                           ChaveamentoService chaveamentoService, GestaoLogRepository gestaoLogRepository,
                           StandingsService standingsService) {
        this.partidaRepository = partidaRepository;
        this.domainEventWriter = domainEventWriter;
        this.chaveamentoService = chaveamentoService;
        this.gestaoLogRepository = gestaoLogRepository;
        this.standingsService = standingsService;
    }

    @Transactional
    public Partida agendar(UUID campeonatoId, UUID groupId,
                            UUID homeTeamId, String homeTeamName,
                            UUID awayTeamId, String awayTeamName,
                            Instant scheduledAt, String local) {
        Partida partida = partidaRepository.save(Partida.agendar(
                campeonatoId, groupId, homeTeamId, homeTeamName, awayTeamId, awayTeamName, scheduledAt, local));

        publicarAgendada(partida);
        return partida;
    }

    @Transactional
    public Partida reagendar(UUID partidaId, Instant novoHorario, String local) {
        Partida partida = buscar(partidaId);
        partida.reagendar(novoHorario, local);
        partidaRepository.save(partida);
        publicarAgendada(partida);
        return partida;
    }

    @Transactional
    public int reagendarEmLote(UUID campeonatoId, int minutos, Sessao ator) {
        if (minutos == 0) {
            throw new IllegalArgumentException("informe um deslocamento diferente de zero (em minutos)");
        }
        List<Partida> afetadas = partidaRepository.findByCampeonatoId(campeonatoId).stream()
                .filter(p -> p.getStatus() == PartidaStatus.AGENDADA && p.getScheduledAt() != null)
                .toList();
        for (Partida partida : afetadas) {
            partida.reagendar(partida.getScheduledAt().plus(Duration.ofMinutes(minutos)));
            partidaRepository.save(partida);
            publicarAgendada(partida);
        }
        String sinal = minutos > 0 ? "+" : "";
        registrarLog(campeonatoId, ator, "REMARCACAO_LOTE",
                "remarcou " + afetadas.size() + " partida(s) em " + sinal + minutos + " min");
        return afetadas.size();
    }

    @Transactional(readOnly = true)
    public List<ConflitoHorario> conflitosDeHorario(UUID campeonatoId) {
        List<Partida> comHorario = partidaRepository.findByCampeonatoId(campeonatoId).stream()
                .filter(p -> p.getStatus() != PartidaStatus.FINALIZADA && p.getScheduledAt() != null)
                .toList();
        List<ConflitoHorario> conflitos = new ArrayList<>();
        for (int i = 0; i < comHorario.size(); i++) {
            for (int j = i + 1; j < comHorario.size(); j++) {
                Partida a = comHorario.get(i);
                Partida b = comHorario.get(j);
                if (!a.getScheduledAt().equals(b.getScheduledAt())) {
                    continue;
                }
                UUID timeEmComum = timeEmComum(a, b);
                if (timeEmComum != null) {
                    conflitos.add(new ConflitoHorario(a.getId(), b.getId(), "TIME",
                            timeEmComum, nomeDoTime(a, timeEmComum), null, a.getScheduledAt()));
                }
                if (mesmoLocal(a, b)) {
                    conflitos.add(new ConflitoHorario(a.getId(), b.getId(), "LOCAL",
                            null, null, a.getLocal(), a.getScheduledAt()));
                }
            }
        }
        return conflitos;
    }

    public record ConflitoHorario(UUID partidaA, UUID partidaB, String tipo,
                                   UUID teamId, String teamName, String local, Instant scheduledAt) {
    }

    private static boolean mesmoLocal(Partida a, Partida b) {
        return a.getLocal() != null && b.getLocal() != null
                && a.getLocal().equalsIgnoreCase(b.getLocal());
    }

    private static UUID timeEmComum(Partida a, Partida b) {
        if (a.getHomeTeamId().equals(b.getHomeTeamId()) || a.getHomeTeamId().equals(b.getAwayTeamId())) {
            return a.getHomeTeamId();
        }
        if (a.getAwayTeamId().equals(b.getHomeTeamId()) || a.getAwayTeamId().equals(b.getAwayTeamId())) {
            return a.getAwayTeamId();
        }
        return null;
    }

    private static String nomeDoTime(Partida partida, UUID teamId) {
        return partida.getHomeTeamId().equals(teamId) ? partida.getHomeTeamName() : partida.getAwayTeamName();
    }

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

    @Transactional
    public Partida atualizarPlacar(UUID partidaId, int homeScore, int awayScore) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new IllegalArgumentException("partida nao encontrada: " + partidaId));

        partida.atualizarPlacar(homeScore, awayScore);
        return partidaRepository.save(partida);
    }

    @Transactional
    public Partida registrarResultado(UUID partidaId, int homeScore, int awayScore, boolean wo, String motivo,
                                       Sessao ator) {
        Partida partida = buscar(partidaId);

        partida.registrarResultado(homeScore, awayScore, wo, motivo);
        partidaRepository.save(partida);

        domainEventWriter.write(partida.getId(), MatchFinishedPayload.TYPE, new MatchFinishedPayload(
                partida.getId(), partida.getCampeonatoId(), partida.getGroupId(),
                new TeamScore(partida.getHomeTeamId(), partida.getHomeTeamName(), partida.getHomeScore()),
                new TeamScore(partida.getAwayTeamId(), partida.getAwayTeamName(), partida.getAwayScore()),
                partida.isWo(), partida.getPlayedAt()));

        // avanço automático de fase (mesma transação): vencedor ocupa o slot
        // seguinte do bracket, grupos completos semeiam os playoffs, e o
        // campeão publica championship.completed.v1
        chaveamentoService.aoFinalizar(partida);

        if (wo) {
            registrarLog(partida.getCampeonatoId(), ator, "WO",
                    "registrou W.O. em " + confronto(partida) + " (" + homeScore + "x" + awayScore + ")"
                            + (motivo != null && !motivo.isBlank() ? " — " + motivo : ""));
        }
        return partida;
    }

    @Transactional
    public Partida corrigirResultado(UUID partidaId, int homeScore, int awayScore, Sessao ator) {
        Partida partida = buscar(partidaId);

        chaveamentoService.validarCorrecao(partida, homeScore, awayScore);

        int anteriorCasa = partida.getHomeScore();
        int anteriorFora = partida.getAwayScore();
        UUID vencedorAntigo = partida.getStage() == com.championship.partidas.domain.PartidaStage.PLAYOFF
                ? partida.vencedorId() : null;

        partida.corrigirResultado(homeScore, awayScore);
        partidaRepository.save(partida);

        domainEventWriter.write(partida.getId(), MatchResultCorrectedPayload.TYPE, new MatchResultCorrectedPayload(
                partida.getId(), partida.getCampeonatoId(), partida.getGroupId(),
                new TeamScore(partida.getHomeTeamId(), partida.getHomeTeamName(), anteriorCasa),
                new TeamScore(partida.getAwayTeamId(), partida.getAwayTeamName(), anteriorFora),
                new TeamScore(partida.getHomeTeamId(), partida.getHomeTeamName(), partida.getHomeScore()),
                new TeamScore(partida.getAwayTeamId(), partida.getAwayTeamName(), partida.getAwayScore()),
                partida.isWo(), Instant.now()));

        if (vencedorAntigo != null) {
            chaveamentoService.repropagarVencedor(partida, vencedorAntigo);
        }

        registrarLog(partida.getCampeonatoId(), ator, "CORRECAO",
                "corrigiu o placar de " + confronto(partida) + ": "
                        + anteriorCasa + "x" + anteriorFora + " → " + homeScore + "x" + awayScore);
        return partida;
    }

    @Transactional
    public int desistirTime(UUID campeonatoId, UUID teamId, Sessao ator) {
        List<Partida> doTime = partidaRepository.findByCampeonatoId(campeonatoId).stream()
                .filter(p -> teamId.equals(p.getHomeTeamId()) || teamId.equals(p.getAwayTeamId()))
                .filter(p -> p.getStatus() != PartidaStatus.FINALIZADA)
                .toList();
        if (doTime.isEmpty()) {
            throw new IllegalStateException("a equipe não tem partidas pendentes para aplicar a desistência");
        }
        String nomeTime = doTime.stream()
                .map(p -> teamId.equals(p.getHomeTeamId()) ? p.getHomeTeamName() : p.getAwayTeamName())
                .findFirst().orElse("equipe");

        for (Partida partida : doTime) {
            if (partida.getStatus() == PartidaStatus.AGENDADA) {
                partida.iniciar();
                domainEventWriter.write(partida.getId(), MatchStartedPayload.TYPE, new MatchStartedPayload(
                        partida.getId(), partida.getCampeonatoId(), partida.getGroupId(), partida.getStartedAt()));
            }
            boolean timeEhCasa = teamId.equals(partida.getHomeTeamId());
            int home = timeEhCasa ? 0 : 1; // adversário vence por W.O.
            int away = timeEhCasa ? 1 : 0;
            partida.registrarResultado(home, away, true, "desistência de " + nomeTime);
            partidaRepository.save(partida);

            domainEventWriter.write(partida.getId(), MatchFinishedPayload.TYPE, new MatchFinishedPayload(
                    partida.getId(), partida.getCampeonatoId(), partida.getGroupId(),
                    new TeamScore(partida.getHomeTeamId(), partida.getHomeTeamName(), partida.getHomeScore()),
                    new TeamScore(partida.getAwayTeamId(), partida.getAwayTeamName(), partida.getAwayScore()),
                    true, partida.getPlayedAt()));
            chaveamentoService.aoFinalizar(partida);
        }

        registrarLog(campeonatoId, ator, "DESISTENCIA",
                "registrou a desistência de \"" + nomeTime + "\" — W.O. em " + doTime.size() + " partida(s) restante(s)");
        return doTime.size();
    }

    @Transactional(readOnly = true)
    public List<GestaoLog> listarGestaoLog(UUID campeonatoId) {
        return gestaoLogRepository.findByCampeonatoIdOrderByCreatedAtDesc(campeonatoId);
    }

    @Transactional(readOnly = true)
    public List<StandingsService.TeamStanding> classificacaoDoGrupo(UUID groupId) {
        return standingsService.classificacaoDoGrupo(groupId);
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

    private void publicarAgendada(Partida partida) {
        domainEventWriter.write(partida.getId(), MatchScheduledPayload.TYPE, new MatchScheduledPayload(
                partida.getId(), partida.getCampeonatoId(), partida.getGroupId(),
                new TeamRef(partida.getHomeTeamId(), partida.getHomeTeamName()),
                new TeamRef(partida.getAwayTeamId(), partida.getAwayTeamName()),
                partida.getScheduledAt(), partida.getLocal()));
    }

    private void registrarLog(UUID campeonatoId, Sessao ator, String acao, String descricao) {
        gestaoLogRepository.save(new GestaoLog(campeonatoId, ator.id(), ator.nome(), acao, descricao));
    }

    private static String confronto(Partida partida) {
        return partida.getHomeTeamName() + " x " + partida.getAwayTeamName();
    }
}
