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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    @Transactional
    public List<Partida> gerar(UUID campeonatoId, FormatoTorneio formato, List<TeamRef> times) {
        return gerar(campeonatoId, formato, times, false);
    }

    public List<Partida> gerar(UUID campeonatoId, FormatoTorneio formato, List<TeamRef> times,
                                boolean disputaTerceiro) {
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
                groupIds == null ? null : json(groupIds.stream().map(UUID::toString).toList()),
                disputaTerceiro && formato != FormatoTorneio.PONTOS_CORRIDOS));

        log.info("confrontos gerados campeonatoId={} formato={} times={} partidas={}",
                campeonatoId, formato, times.size(), criadas.size());
        return criadas;
    }

    @Transactional(readOnly = true)
    public List<ChaveSlot> listarSlots(UUID campeonatoId) {
        return chaveSlotRepository.findByIdCampeonatoId(campeonatoId);
    }

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

    @Transactional
    public void aoFinalizar(Partida partida) {
        Optional<TorneioChaveamento> configuracao = chaveamentoRepository.findById(partida.getCampeonatoId());
        if (configuracao.isEmpty()) {
            return; // campeonato legado, sem sorteio gerado
        }
        TorneioChaveamento config = configuracao.get();

        if (partida.getStage() == PartidaStage.PLAYOFF) {
            if (partida.isTerceiroLugar()) {
                return; // disputa de 3º lugar é terminal: não avança nem coroa campeão
            }
            TeamRef vencedor = new TeamRef(partida.vencedorId(), partida.vencedorNome());
            if (partida.getRound().equals(config.getTotalRounds())) {
                encerrarCampeonato(partida.getCampeonatoId(), vencedor);
            } else {
                ocuparSlot(partida.getCampeonatoId(), partida.getRound() + 1, partida.getBracketPos(),
                        vencedor, new ArrayList<>());
                criarDisputaTerceiroSePronta(partida, config);
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
            StandingsService.TeamStanding lider = StandingsService.ordenar(deGrupos, ordemDoSorteio(config)).get(0);
            encerrarCampeonato(partida.getCampeonatoId(), new TeamRef(lider.teamId(), lider.teamName()));
        } else if (config.getFormato() == FormatoTorneio.GRUPOS_PLAYOFFS) {
            boolean playoffsJaSemeados = doCampeonato.stream()
                    .anyMatch(p -> p.getStage() == PartidaStage.PLAYOFF);
            if (!playoffsJaSemeados) {
                semearPlayoffs(partida.getCampeonatoId(), config, deGrupos);
            }
        }
    }

    // ---- correção de resultado registrado (item de gestão) ----

    @Transactional(readOnly = true)
    public void validarCorrecao(Partida partida, int novoHome, int novoAway) {
        Optional<TorneioChaveamento> configuracao = chaveamentoRepository.findById(partida.getCampeonatoId());
        if (configuracao.isEmpty()) {
            return; // campeonato legado sem sorteio: correção livre
        }
        TorneioChaveamento config = configuracao.get();
        List<Partida> doCampeonato = partidaRepository.findByCampeonatoId(partida.getCampeonatoId());

        if (partida.getStage() == PartidaStage.PLAYOFF) {
            UUID vencedorAtual = partida.vencedorId();
            UUID novoVencedor = novoHome > novoAway ? partida.getHomeTeamId() : partida.getAwayTeamId();
            if (vencedorAtual.equals(novoVencedor)) {
                return; // vencedor não muda: correção cosmética, sempre segura
            }
            if (partida.getRound().equals(config.getTotalRounds())) {
                throw new IllegalStateException(
                        "esta é a final e o campeão já foi definido — para trocar o vencedor da final, "
                                + "recrie o torneio (correção da final não é suportada)");
            }
            proximaPartida(doCampeonato, partida).ifPresent(proxima -> {
                if (proxima.getStatus() != PartidaStatus.AGENDADA) {
                    throw new IllegalStateException(
                            "o próximo jogo do vencedor (" + proxima.getHomeTeamName() + " x "
                                    + proxima.getAwayTeamName() + ") já começou — não é possível trocar quem avançou");
                }
            });
            return;
        }

        // fase de grupos / pontos corridos
        boolean playoffsSemeados = doCampeonato.stream().anyMatch(p -> p.getStage() == PartidaStage.PLAYOFF);
        if (config.getFormato() == FormatoTorneio.GRUPOS_PLAYOFFS && playoffsSemeados) {
            throw new IllegalStateException(
                    "a fase de grupos já foi encerrada e o mata-mata sorteado — não é possível corrigir resultados de grupo");
        }
        if (config.getFormato() == FormatoTorneio.PONTOS_CORRIDOS) {
            boolean todasFinalizadas = doCampeonato.stream()
                    .filter(p -> p.getStage() == PartidaStage.GRUPOS)
                    .allMatch(p -> p.getStatus() == PartidaStatus.FINALIZADA);
            if (todasFinalizadas) {
                throw new IllegalStateException(
                        "o torneio já foi encerrado com campeão — não é possível corrigir o resultado");
            }
        }
    }

    @Transactional
    public void repropagarVencedor(Partida partida, UUID vencedorAntigoId) {
        if (partida.getStage() != PartidaStage.PLAYOFF) {
            return;
        }
        if (partida.vencedorId().equals(vencedorAntigoId)) {
            return; // vencedor não mudou
        }
        int proximaRodada = partida.getRound() + 1;
        int slot = partida.getBracketPos();
        TeamRef novoVencedor = new TeamRef(partida.vencedorId(), partida.vencedorNome());

        chaveSlotRepository.save(new ChaveSlot(
                partida.getCampeonatoId(), proximaRodada, slot, novoVencedor.teamId(), novoVencedor.name()));

        List<Partida> doCampeonato = partidaRepository.findByCampeonatoId(partida.getCampeonatoId());
        proximaPartida(doCampeonato, partida).ifPresent(proxima -> {
            boolean casa = slot % 2 == 0; // slot par alimenta o time da casa
            proxima.substituirTime(casa, novoVencedor.teamId(), novoVencedor.name());
            partidaRepository.save(proxima);
            publicarAgendada(proxima);
            log.info("bracket repropagado apos correcao campeonatoId={} rodada={} novoVencedor={}",
                    partida.getCampeonatoId(), proximaRodada, novoVencedor.name());
        });
    }

    private void criarDisputaTerceiroSePronta(Partida semifinal, TorneioChaveamento config) {
        if (!config.isDisputaTerceiro() || config.getTotalRounds() == null) {
            return;
        }
        int rodadaSemis = config.getTotalRounds() - 1;
        if (semifinal.getRound() == null || semifinal.getRound() != rodadaSemis) {
            return;
        }
        List<Partida> doCampeonato = partidaRepository.findByCampeonatoId(semifinal.getCampeonatoId());
        List<Partida> semis = doCampeonato.stream()
                .filter(p -> p.getStage() == PartidaStage.PLAYOFF && !p.isTerceiroLugar())
                .filter(p -> p.getRound() != null && p.getRound() == rodadaSemis)
                .toList();
        boolean todasFinalizadas = semis.size() == 2
                && semis.stream().allMatch(p -> p.getStatus() == PartidaStatus.FINALIZADA);
        boolean jaExiste = doCampeonato.stream().anyMatch(Partida::isTerceiroLugar);
        if (!todasFinalizadas || jaExiste) {
            return;
        }
        Partida a = semis.get(0);
        Partida b = semis.get(1);
        Partida terceiro = partidaRepository.save(Partida.deTerceiroLugar(
                semifinal.getCampeonatoId(), config.getTotalRounds(),
                a.perdedorId(), a.perdedorNome(), b.perdedorId(), b.perdedorNome()));
        publicarAgendada(terceiro);
        log.info("disputa de 3o lugar criada campeonatoId={} {} x {}",
                semifinal.getCampeonatoId(), a.perdedorNome(), b.perdedorNome());
    }

    private Optional<Partida> proximaPartida(List<Partida> doCampeonato, Partida partida) {
        int proximaRodada = partida.getRound() + 1;
        int proximoBracketPos = partida.getBracketPos() / 2;
        return doCampeonato.stream()
                .filter(p -> p.getStage() == PartidaStage.PLAYOFF)
                .filter(p -> p.getRound() != null && p.getRound() == proximaRodada)
                .filter(p -> p.getBracketPos() != null && p.getBracketPos() == proximoBracketPos)
                .findFirst();
    }

    // ---- semeadura dos playoffs a partir dos grupos ----

    private void semearPlayoffs(UUID campeonatoId, TorneioChaveamento config, List<Partida> deGrupos) {
        List<UUID> groupIds = lerJson(config.getGroupIds()).stream().map(UUID::fromString).toList();
        List<UUID> ordemSorteio = ordemDoSorteio(config);

        Map<UUID, List<Partida>> porGrupo = new HashMap<>();
        for (Partida p : deGrupos) {
            porGrupo.computeIfAbsent(p.getGroupId(), key -> new ArrayList<>()).add(p);
        }

        List<List<TeamRef>> classificados = new ArrayList<>();
        for (UUID groupId : groupIds) {
            List<TeamRef> classificacao = StandingsService.ordenar(porGrupo.get(groupId), ordemSorteio).stream()
                    .map(s -> new TeamRef(s.teamId(), s.teamName()))
                    .toList();
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

    // ---- mecânica do bracket ----

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
        // organizado por rodada (método do círculo): o organizador agenda e
        // comunica a liga em ondas ("Rodada 1, Rodada 2…")
        List<List<int[]>> rodadas = Chaveamento.todosContraTodosPorRodada(grupo.size());
        for (int r = 0; r < rodadas.size(); r++) {
            for (int[] par : rodadas.get(r)) {
                TeamRef casa = grupo.get(par[0]);
                TeamRef fora = grupo.get(par[1]);
                Partida partida = partidaRepository.save(Partida.deGrupo(
                        campeonatoId, groupId, r + 1,
                        casa.teamId(), casa.name(),
                        fora.teamId(), fora.name()));
                criadas.add(partida);
                publicarAgendada(partida);
            }
        }
    }

    private void publicarAgendada(Partida partida) {
        domainEventWriter.write(partida.getId(), MatchScheduledPayload.TYPE, new MatchScheduledPayload(
                partida.getId(), partida.getCampeonatoId(), partida.getGroupId(),
                new TeamRef(partida.getHomeTeamId(), partida.getHomeTeamName()),
                new TeamRef(partida.getAwayTeamId(), partida.getAwayTeamName()),
                partida.getScheduledAt(), partida.getLocal()));
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
