package com.championship.partidas.application;

import com.championship.partidas.domain.Partida;
import com.championship.partidas.domain.PartidaStatus;
import com.championship.partidas.infrastructure.persistence.PartidaRepository;
import com.championship.partidas.infrastructure.persistence.TorneioChaveamento;
import com.championship.partidas.infrastructure.persistence.TorneioChaveamentoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class StandingsService {

    private final PartidaRepository partidaRepository;
    private final TorneioChaveamentoRepository chaveamentoRepository;
    private final ObjectMapper objectMapper;

    public StandingsService(PartidaRepository partidaRepository,
                             TorneioChaveamentoRepository chaveamentoRepository,
                             ObjectMapper objectMapper) {
        this.partidaRepository = partidaRepository;
        this.chaveamentoRepository = chaveamentoRepository;
        this.objectMapper = objectMapper;
    }

    public record TeamStanding(UUID teamId, String teamName, int pontos, int vitorias, int empates,
                                int derrotas, int pro, int contra, String desempate) {
        public int saldo() {
            return pro - contra;
        }
    }

    @Transactional(readOnly = true)
    public List<TeamStanding> classificacaoDoGrupo(UUID groupId) {
        List<Partida> doGrupo = partidaRepository.findByGroupId(groupId);
        if (doGrupo.isEmpty()) {
            return List.of();
        }
        List<UUID> drawOrder = ordemDoSorteio(doGrupo.get(0).getCampeonatoId());
        return ordenar(doGrupo, drawOrder);
    }

    public static List<TeamStanding> ordenar(List<Partida> partidasDoGrupo, List<UUID> drawOrder) {
        Map<UUID, Acc> acc = new LinkedHashMap<>();
        for (Partida p : partidasDoGrupo) {
            acc.computeIfAbsent(p.getHomeTeamId(), k -> new Acc()).nome = p.getHomeTeamName();
            acc.computeIfAbsent(p.getAwayTeamId(), k -> new Acc()).nome = p.getAwayTeamName();
        }
        List<Partida> finalizadas = partidasDoGrupo.stream()
                .filter(p -> p.getStatus() == PartidaStatus.FINALIZADA).toList();
        for (Partida p : finalizadas) {
            aplicar(acc, p);
        }

        List<UUID> ids = new ArrayList<>(acc.keySet());
        // ordena primeiro por pontos (desc), estável
        ids.sort((a, b) -> Integer.compare(acc.get(b).pontos, acc.get(a).pontos));

        // resolve cada faixa de pontos empatados com os critérios seguintes
        List<TeamStanding> resultado = new ArrayList<>();
        int i = 0;
        while (i < ids.size()) {
            int j = i;
            while (j < ids.size() && acc.get(ids.get(j)).pontos == acc.get(ids.get(i)).pontos) {
                j++;
            }
            List<UUID> faixa = new ArrayList<>(ids.subList(i, j));
            if (faixa.size() > 1) {
                ordenarFaixaEmpatada(faixa, acc, finalizadas, drawOrder, resultado);
            } else {
                resultado.add(linha(faixa.get(0), acc, null));
            }
            i = j;
        }
        return resultado;
    }

    private static void ordenarFaixaEmpatada(List<UUID> faixa, Map<UUID, Acc> acc, List<Partida> finalizadas,
                                       List<UUID> drawOrder, List<TeamStanding> saida) {
        // confronto direto: mini-tabela de pontos considerando só jogos entre os empatados
        Map<UUID, Integer> h2h = new java.util.HashMap<>();
        for (UUID id : faixa) {
            h2h.put(id, 0);
        }
        for (Partida p : finalizadas) {
            if (!faixa.contains(p.getHomeTeamId()) || !faixa.contains(p.getAwayTeamId())) {
                continue;
            }
            int hs = p.getHomeScore(), as = p.getAwayScore();
            if (hs > as) {
                h2h.merge(p.getHomeTeamId(), 3, Integer::sum);
            } else if (as > hs) {
                h2h.merge(p.getAwayTeamId(), 3, Integer::sum);
            } else {
                h2h.merge(p.getHomeTeamId(), 1, Integer::sum);
                h2h.merge(p.getAwayTeamId(), 1, Integer::sum);
            }
        }

        faixa.sort((a, b) -> {
            int c = Integer.compare(h2h.get(b), h2h.get(a));
            if (c != 0) return c;
            c = Integer.compare(acc.get(b).saldo(), acc.get(a).saldo());
            if (c != 0) return c;
            c = Integer.compare(acc.get(b).pro, acc.get(a).pro);
            if (c != 0) return c;
            c = Integer.compare(acc.get(b).vitorias, acc.get(a).vitorias);
            if (c != 0) return c;
            return Integer.compare(indice(drawOrder, a), indice(drawOrder, b));
        });

        for (int k = 0; k < faixa.size(); k++) {
            UUID id = faixa.get(k);
            String desempate = k == 0 ? null : criterioQueSeparou(faixa.get(k - 1), id, acc, h2h, drawOrder);
            saida.add(linha(id, acc, desempate));
        }
    }

    private static String criterioQueSeparou(UUID prev, UUID cur, Map<UUID, Acc> acc,
                                        Map<UUID, Integer> h2h, List<UUID> drawOrder) {
        if (!h2h.get(prev).equals(h2h.get(cur))) return "confronto direto";
        if (acc.get(prev).saldo() != acc.get(cur).saldo()) return "saldo";
        if (acc.get(prev).pro != acc.get(cur).pro) return "pró";
        if (acc.get(prev).vitorias != acc.get(cur).vitorias) return "vitórias";
        return "sorteio";
    }

    private static TeamStanding linha(UUID id, Map<UUID, Acc> acc, String desempate) {
        Acc a = acc.get(id);
        return new TeamStanding(id, a.nome, a.pontos, a.vitorias, a.empates, a.derrotas, a.pro, a.contra, desempate);
    }

    private static void aplicar(Map<UUID, Acc> acc, Partida p) {
        Acc casa = acc.get(p.getHomeTeamId());
        Acc fora = acc.get(p.getAwayTeamId());
        int hs = p.getHomeScore(), as = p.getAwayScore();
        if (!p.isWo()) {
            // W.O. é neutro no placar (não soma pró/contra)
            casa.pro += hs;
            casa.contra += as;
            fora.pro += as;
            fora.contra += hs;
        }
        if (hs > as) {
            casa.pontos += 3;
            casa.vitorias++;
            fora.derrotas++;
        } else if (as > hs) {
            fora.pontos += 3;
            fora.vitorias++;
            casa.derrotas++;
        } else {
            casa.pontos++;
            fora.pontos++;
            casa.empates++;
            fora.empates++;
        }
    }

    private static int indice(List<UUID> drawOrder, UUID id) {
        int idx = drawOrder.indexOf(id);
        return idx < 0 ? Integer.MAX_VALUE : idx;
    }

    private List<UUID> ordemDoSorteio(UUID campeonatoId) {
        Optional<TorneioChaveamento> config = chaveamentoRepository.findById(campeonatoId);
        if (config.isEmpty() || config.get().getDrawOrder() == null) {
            return List.of();
        }
        try {
            List<String> ids = objectMapper.readValue(config.get().getDrawOrder(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return ids.stream().map(UUID::fromString).toList();
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static final class Acc {
        String nome = "";
        int pontos, vitorias, empates, derrotas, pro, contra;

        int saldo() {
            return pro - contra;
        }
    }
}
