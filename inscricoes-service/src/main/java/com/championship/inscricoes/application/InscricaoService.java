package com.championship.inscricoes.application;

import com.championship.inscricoes.domain.Campeonato;
import com.championship.inscricoes.domain.Inscricao;
import com.championship.inscricoes.domain.Time;
import com.championship.inscricoes.infrastructure.messaging.DomainEventWriter;
import com.championship.inscricoes.infrastructure.messaging.events.PlayerRef;
import com.championship.inscricoes.infrastructure.messaging.events.TeamRegisteredPayload;
import com.championship.inscricoes.infrastructure.persistence.CampeonatoRepository;
import com.championship.inscricoes.infrastructure.persistence.InscricaoRepository;
import com.championship.inscricoes.infrastructure.persistence.TimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InscricaoService {

    private final CampeonatoRepository campeonatoRepository;
    private final TimeRepository timeRepository;
    private final InscricaoRepository inscricaoRepository;
    private final DomainEventWriter domainEventWriter;

    public InscricaoService(CampeonatoRepository campeonatoRepository,
                             TimeRepository timeRepository,
                             InscricaoRepository inscricaoRepository,
                             DomainEventWriter domainEventWriter) {
        this.campeonatoRepository = campeonatoRepository;
        this.timeRepository = timeRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.domainEventWriter = domainEventWriter;
    }

    @Transactional
    public Campeonato criarCampeonato(String nome) {
        return campeonatoRepository.save(Campeonato.criar(nome));
    }

    @Transactional(readOnly = true)
    public List<Campeonato> listarCampeonatos() {
        return campeonatoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Inscricao> listarInscricoes(UUID campeonatoId) {
        return inscricaoRepository.findDetalhadoByCampeonatoId(campeonatoId);
    }

    /**
     * Cria o time e sua inscrição pendente num campeonato, e grava
     * team.registered.v1 no outbox na mesma transação — dispara a saga.
     */
    @Transactional
    public Inscricao inscreverTime(UUID campeonatoId, String nomeTime, List<String> nomesJogadores) {
        Campeonato campeonato = campeonatoRepository.findById(campeonatoId)
                .orElseThrow(() -> new IllegalArgumentException("campeonato nao encontrado: " + campeonatoId));
        if (!campeonato.aceitaInscricoes()) {
            throw new IllegalStateException("campeonato nao aceita inscricoes: " + campeonatoId);
        }
        if (inscricaoRepository.existsByCampeonatoIdAndNomeTime(campeonatoId, nomeTime)) {
            throw new IllegalStateException(
                    "time '" + nomeTime + "' ja esta inscrito neste campeonato — use o time existente ao agendar partidas");
        }

        Time time = timeRepository.save(Time.criar(nomeTime, nomesJogadores));
        Inscricao inscricao = inscricaoRepository.save(Inscricao.pendente(time, campeonato));

        List<PlayerRef> players = time.getJogadores().stream()
                .map(jogador -> new PlayerRef(jogador.getId(), jogador.getNome()))
                .toList();

        domainEventWriter.write(time.getId(), TeamRegisteredPayload.TYPE,
                new TeamRegisteredPayload(time.getId(), time.getNome(), campeonato.getId(), players));

        return inscricao;
    }
}
