package com.championship.partidas.application;

import com.championship.partidas.domain.Chaveamento;
import com.championship.partidas.domain.FormatoTorneio;
import com.championship.partidas.domain.Partida;
import com.championship.partidas.domain.PartidaStage;
import com.championship.partidas.domain.PartidaStatus;
import com.championship.partidas.infrastructure.messaging.DomainEventWriter;
import com.championship.partidas.infrastructure.messaging.events.ChampionshipCompletedPayload;
import com.championship.partidas.infrastructure.messaging.events.MatchScheduledPayload;
import com.championship.partidas.infrastructure.messaging.events.TeamRef;
import com.championship.partidas.infrastructure.persistence.ChaveSlot;
import com.championship.partidas.infrastructure.persistence.ChaveSlot.ChaveSlotId;
import com.championship.partidas.infrastructure.persistence.ChaveSlotRepository;
import com.championship.partidas.infrastructure.persistence.PartidaRepository;
import com.championship.partidas.infrastructure.persistence.TorneioChaveamento;
import com.championship.partidas.infrastructure.persistence.TorneioChaveamentoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Geração de confrontos e avanço automático de fase (ver especificação de
 * formatos). A classificação dos grupos é calculada aqui, a partir dos
 * resultados que este serviço já possui — nunca há chamada síncrona ao
 * ranking-service (CLAUDE.md "o que não fazer"); o ranking segue sendo o
 * read model de exibição, alimentado por match.finished.v1.
 */
@Service
public class ChaveamentoService {

    private static final Logger log = LoggerFactory.getLogger(ChaveamentoService.class);

    private final PartidaRepository partidaRepository;
    private final TorneioChaveamentoRepository chaveamentoRepository;
    private final ChaveSlotRepository chaveSlotRepository;
    private final DomainEventWriter domainEventWriter;
    private final ObjectMapper objectMapper;

    public ChaveamentoService(PartidaRepository partidaRepository,
                               TorneioChaveamentoRepository chaveamentoRepository,
                               ChaveSlotRepository chaveSlotRepository,
                               DomainEventWriter domainEventWriter,
                               ObjectMapper objectMapper) {
        this.partidaRepository = partidaRepository;
        this.chaveamentoRepository = chaveamentoRepository;
        this.chaveSlotRepository = chaveSlotRepository;
        this.domainEventWriter = domainEventWriter;
        this.objectMapper = objectMapper;
    }

    /**
     * Sorteia os times e gera todas as partidas da(s) primeira(s) fase(s).
     * Re-sortear é permitido enquanto nenhuma partida tiver sido iniciada:
     * descarta tudo e regenera.
     */
    @Transactional
    public List<Partida> gerar(UUID campeonatoId, FormatoTorneio formato, List<TeamRef> times) {
        validarTimes(formato, times);
        descartarSorteio(campeonatoId);

        List<TeamRef> sorteados = new ArrayList<>(times);
        java.util.Collections.shuffle(sorteados, ThreadLocalRandom.current());

        List<Partida> criadas = new ArrayList<>();
        Integer totalRounds = null;
        List<UUID> groupIds = null;

        switch (formato) {
            case PONTOS_CORRIDOS -> {
                UUID groupId = UUID.randomUUID();
                groupIds = List.of(groupId);
                criarPartidasDeGrupo(campeonatoId, groupId, sorteados, criadas);
            }
            case GRUPOS_PLAYOFFS -> {
                int numeroDeGrupos = Chaveamento.numeroDeGrupos(sorteados.size());
                List<List<TeamRef>> grupos = Chaveamento.distribuirEmGrupos(sorteados, numeroDeGrupos);
                groupIds = new ArrayList<>();
                for (List<TeamRef> grupo : grupos) {
                    UUID groupId = UUID.randomUUID();
                    groupIds.add(groupId);
                    criarPartidasDeGrupo(campeonatoId, groupId, grupo, criadas);
                }
                // classificam 2 por grupo -> bracket perfeito de 2G times
                totalRounds = Chaveamento.rodadas(2 * numeroDeGrupos);
            }
            case PLAYOFFS -> {
                int tamanhoBracket = Chaveamento.proximaPotenciaDe2(sorteados.size());
                totalRounds = Chaveamento.rodadas(tamanhoBracket);
                int byes = tamanhoBracket - sorteados.size();
                List<Integer> slotsDeBye = Chaveamento.slotsDeBye(tamanhoBracket, byes);

                // os primeiros da ordem sorteada recebem bye: entram direto na 2ª rodada
                for (int i = 0; i < byes; i++) {
                    ocuparSlot(campeonatoId, 2, slotsDeBye.get(i), sorteados.get(i), criadas);
                }
                // demais times preenchem as posições da 1ª rodada que alimentam slots sem bye
                List<Integer> posicoesLivres = new ArrayList<>();
                for (int pos = 0; pos < tamanhoBracket / 2; pos++) {
                    if (!slotsDeBye.contains(pos)) {
                        posicoesLivres.add(pos);
                    }
                }
                int proximoTime = byes;
                for (int pos : posicoesLivres) {
                    ocuparSlot(campeonatoId, 1, 2 * pos, sorteados.get(proximoTime++), criadas);
                    ocuparSlot(campeonatoId, 1, 2 * pos + 1, sorteados.get(proximoTime++), criadas);
                }
            }
        }

        chaveamentoRepository.save(new TorneioChaveamento(
                campeonatoId, formato, totalRounds,
                json(sorteados.stream().map(time -> time.teamId().toString()).toList()),
                groupIds == null ? null : json(groupIds.stream().map(UUID::toString).toList())));

        log.info("confrontos gerados campeonatoId={} formato={} times={} partidas={}",
                campeonatoId, formato, times.size(), criadas.size());
        return criadas;
    }

    /**
     * Descarta o sorteio (re-sortear / reabrir inscrições). Bloqueado se
     * alguma partida do campeonato já saiu de AGENDADA.
     */
    @Transactional
    public void descartarSorteio(UUID campeonatoId) {
        List<Partida> existentes = partidaRepository.findByCampeonatoId(campeonatoId);
        boolean torneioComecou = existentes.stream().anyMatch(partida -> partida.getStatus() != PartidaStatus.AGENDADA);
        if (torneioComecou) {
            throw new IllegalStateException(
                    "o torneio ja tem partidas iniciadas ou encerradas — o sorteio nao pode ser descartado");
        }
        partidaRepository.deleteAll(existentes);
        chaveSlotRepository.deleteByIdCampeonatoId(campeonatoId);
        chaveamentoRepository.deleteById(campeonatoId);
    }

    /**
     * Avanço automático de fase, chamado na mesma transação do registro de
     * resultado: vencedor de mata-mata ocupa o slot da rodada seguinte; fim
     * da fase de grupos semeia os playoffs; e o campeão encerra o torneio
     * via championship.completed.v1 (outbox).
     */
    @Transactional
    public void aoFinalizar(Partida partida) {
        Optional<TorneioChaveamento> configuracao = chaveamentoRepository.findById(partida.getCampeonatoId());
        if (configuracao.isEmpty()) {
            return; // campeonato legado, sem sorteio gerado
        }
        TorneioChaveamento config = configuracao.get();

        if (partida.getStage() == PartidaStage.PLAYOFF) {
            TeamRef vencedor = new TeamRef(partida.vencedorId(), partida.vencedorNome());
            if (partida.getRound().equals(config.getTotalRounds())) {
                encerrarCampeonato(partida.getCampeonatoId(), vencedor);
            } else {
                ocuparSlot(partida.getCampeonatoId(), partida.getRound() + 1, partida.getBracketPos(),
                        vencedor, new ArrayList<>());
            }
            return;
        }

        // fase de grupos / pontos corridos: reage quando a fase termina
        List<Partida> doCampeonato = partidaRepository.findByCampeonatoId(partida.getCampeonatoId());
        List<Partida> deGrupos = doCampeonato.stream()
                .filter(p -> p.getStage() == PartidaStage.GRUPOS).toList();
        boolean faseCompleta = deGrupos.stream().allMatch(p -> p.getStatus() == PartidaStatus.FINALIZADA);
        if (!faseCompleta) {
            return;
        }

        if (config.getFormato() == FormatoTorneio.PONTOS_CORRIDOS) {
            TeamRef campeao = classificar(deGrupos, ordemDoSorteio(config)).get(0);
            encerrarCampeonato(partida.getCampeonatoId(), campeao);
        } else if (config.getFormato() == FormatoTorneio.GRUPOS_PLAYOFFS) {
            boolean playoffsJaSemeados = doCampeonato.stream()
                    .anyMatch(p -> p.getStage() == PartidaStage.PLAYOFF);
            if (!playoffsJaSemeados) {
                semearPlayoffs(partida.getCampeonatoId(), config, deGrupos);
            }
        }
    }

    // ---- semeadura dos playoffs a partir dos grupos ----

    /**
     * Seeding cruzado pareando grupos vizinhos: com grupos (X, Y), a metade de
     * cima do bracket recebe 1ºX × 2ºY e a de baixo 1ºY × 2ºX — times do mesmo
     * grupo só se reencontram na final.
     */
    private void semearPlayoffs(UUID campeonatoId, TorneioChaveamento config, List<Partida> deGrupos) {
        List<UUID> groupIds = lerJson(config.getGroupIds()).stream().map(UUID::fromString).toList();
        List<UUID> ordemSorteio = ordemDoSorteio(config);

        Map<UUID, List<Partida>> porGrupo = new HashMap<>();
        for (Partida p : deGrupos) {
            porGrupo.computeIfAbsent(p.getGroupId(), key -> new ArrayList<>()).add(p);
        }

        List<List<TeamRef>> classificados = new ArrayList<>();
        for (UUID groupId : groupIds) {
            List<TeamRef> classificacao = classificar(porGrupo.get(groupId), ordemSorteio);
            classificados.add(classificacao.subList(0, 2));
        }

        int pares = groupIds.size() / 2;
        List<Partida> criadas = new ArrayList<>();
        for (int par = 0; par < pares; par++) {
            List<TeamRef> grupoX = classificados.get(2 * par);
            List<TeamRef> grupoY = classificados.get(2 * par + 1);
            int posCima = par;
            int posBaixo = pares + par;
            ocuparSlot(campeonatoId, 1, 2 * posCima, grupoX.get(0), criadas);
            ocuparSlot(campeonatoId, 1, 2 * posCima + 1, grupoY.get(1), criadas);
            ocuparSlot(campeonatoId, 1, 2 * posBaixo, grupoY.get(0), criadas);
            ocuparSlot(campeonatoId, 1, 2 * posBaixo + 1, grupoX.get(1), criadas);
        }
        log.info("playoffs semeados campeonatoId={} confrontos={}", campeonatoId, criadas.size());
    }

    /**
     * Classificação calculada dos resultados locais. Desempate: pontos,
     * vitórias, saldo, gols pró e, por fim, a ordem do sorteio (determinística).
     */
    private static List<TeamRef> classificar(List<Partida> finalizadas, List<UUID> ordemSorteio) {
        Map<UUID, int[]> estatisticas = new HashMap<>(); // [pontos, vitorias, saldo, golsPro]
        Map<UUID, String> nomes = new HashMap<>();

        for (Partida p : finalizadas) {
            nomes.put(p.getHomeTeamId(), p.getHomeTeamName());
            nomes.put(p.getAwayTeamId(), p.getAwayTeamName());
            int[] casa = estatisticas.computeIfAbsent(p.getHomeTeamId(), key -> new int[4]);
            int[] fora = estatisticas.computeIfAbsent(p.getAwayTeamId(), key -> new int[4]);
            int golsCasa = p.getHomeScore();
            int golsFora = p.getAwayScore();
            casa[2] += golsCasa - golsFora;
            fora[2] += golsFora - golsCasa;
            casa[3] += golsCasa;
            fora[3] += golsFora;
            if (golsCasa > golsFora) {
                casa[0] += 3;
                casa[1] += 1;
            } else if (golsFora > golsCasa) {
                fora[0] += 3;
                fora[1] += 1;
            } else {
                casa[0] += 1;
                fora[0] += 1;
            }
        }

        Comparator<UUID> criterios = Comparator
                .<UUID>comparingInt(id -> estatisticas.get(id)[0]).reversed()
                .thenComparing(Comparator.<UUID>comparingInt(id -> estatisticas.get(id)[1]).reversed())
                .thenComparing(Comparator.<UUID>comparingInt(id -> estatisticas.get(id)[2]).reversed())
                .thenComparing(Comparator.<UUID>comparingInt(id -> estatisticas.get(id)[3]).reversed())
                .thenComparingInt(ordemSorteio::indexOf);

        return estatisticas.keySet().stream()
                .sorted(criterios)
                .map(id -> new TeamRef(id, nomes.get(id)))
                .toList();
    }

    // ---- mecânica do bracket ----

    /**
     * Ocupa um slot do bracket; quando o slot irmão (mesmo confronto) já está
     * ocupado, cria a partida da rodada e publica match.scheduled.v1.
     */
    private void ocuparSlot(UUID campeonatoId, int round, int slot, TeamRef time, List<Partida> criadas) {
        chaveSlotRepository.save(new ChaveSlot(campeonatoId, round, slot, time.teamId(), time.name()));

        int slotIrmao = slot ^ 1;
        Optional<ChaveSlot> irmao = chaveSlotRepository.findById(new ChaveSlotId(campeonatoId, round, slotIrmao));
        if (irmao.isEmpty()) {
            return;
        }

        TeamRef doSlotPar = slot % 2 == 0 ? time : new TeamRef(irmao.get().getTeamId(), irmao.get().getTeamName());
        TeamRef doSlotImpar = slot % 2 == 0 ? new TeamRef(irmao.get().getTeamId(), irmao.get().getTeamName()) : time;

        Partida partida = partidaRepository.save(Partida.dePlayoff(
                campeonatoId, round, slot / 2,
                doSlotPar.teamId(), doSlotPar.name(),
                doSlotImpar.teamId(), doSlotImpar.name()));
        criadas.add(partida);
        publicarAgendada(partida);
    }

    private void criarPartidasDeGrupo(UUID campeonatoId, UUID groupId, List<TeamRef> grupo, List<Partida> criadas) {
        for (int[] par : Chaveamento.todosContraTodos(grupo.size())) {
            TeamRef casa = grupo.get(par[0]);
            TeamRef fora = grupo.get(par[1]);
            Partida partida = partidaRepository.save(Partida.agendar(
                    campeonatoId, groupId,
                    casa.teamId(), casa.name(),
                    fora.teamId(), fora.name(),
                    null));
            criadas.add(partida);
            publicarAgendada(partida);
        }
    }

    private void publicarAgendada(Partida partida) {
        domainEventWriter.write(partida.getId(), MatchScheduledPayload.TYPE, new MatchScheduledPayload(
                partida.getId(), partida.getCampeonatoId(), partida.getGroupId(),
                new TeamRef(partida.getHomeTeamId(), partida.getHomeTeamName()),
                new TeamRef(partida.getAwayTeamId(), partida.getAwayTeamName()),
                partida.getScheduledAt()));
    }

    private void encerrarCampeonato(UUID campeonatoId, TeamRef campeao) {
        domainEventWriter.write(campeonatoId, ChampionshipCompletedPayload.TYPE,
                new ChampionshipCompletedPayload(campeonatoId, campeao, Instant.now()));
        log.info("campeao definido campeonatoId={} campeao={}", campeonatoId, campeao.name());
    }

    // ---- validações e utilidades ----

    private static void validarTimes(FormatoTorneio formato, List<TeamRef> times) {
        if (times == null || times.size() < formato.minimoDeTimes()) {
            throw new IllegalArgumentException(
                    "formato " + formato + " exige pelo menos " + formato.minimoDeTimes() + " times confirmados");
        }
        Set<UUID> ids = new HashSet<>();
        for (TeamRef time : times) {
            if (!ids.add(time.teamId())) {
                throw new IllegalArgumentException("time duplicado no sorteio: " + time.teamId());
            }
        }
    }

    private List<UUID> ordemDoSorteio(TorneioChaveamento config) {
        return lerJson(config.getDrawOrder()).stream().map(UUID::fromString).toList();
    }

    private String json(List<String> valores) {
        try {
            return objectMapper.writeValueAsString(valores);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("falha ao serializar configuracao do sorteio", e);
        }
    }

    private List<String> lerJson(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("falha ao ler configuracao do sorteio", e);
        }
    }
}
