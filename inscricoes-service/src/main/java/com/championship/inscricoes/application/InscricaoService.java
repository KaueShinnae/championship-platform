package com.championship.inscricoes.application;

import com.championship.inscricoes.domain.Campeonato;
import com.championship.inscricoes.domain.CampeonatoAdmin;
import com.championship.inscricoes.domain.CampeonatoAdmin.CampeonatoAdminId;
import com.championship.inscricoes.domain.CampeonatoFormato;
import com.championship.inscricoes.domain.Inscricao;
import com.championship.inscricoes.domain.InscricaoStatus;
import com.championship.inscricoes.domain.Time;
import com.championship.inscricoes.domain.Usuario;
import com.championship.inscricoes.infrastructure.messaging.DomainEventWriter;
import com.championship.inscricoes.infrastructure.messaging.events.ChampionshipPermissionsChangedPayload;
import com.championship.inscricoes.infrastructure.messaging.events.PlayerRef;
import com.championship.inscricoes.infrastructure.messaging.events.TeamRegisteredPayload;
import com.championship.inscricoes.infrastructure.persistence.CampeonatoAdminRepository;
import com.championship.inscricoes.infrastructure.persistence.CampeonatoRepository;
import com.championship.inscricoes.infrastructure.persistence.InscricaoRepository;
import com.championship.inscricoes.infrastructure.persistence.TimeRepository;
import com.championship.inscricoes.infrastructure.persistence.UsuarioRepository;
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
    private final CampeonatoAdminRepository campeonatoAdminRepository;
    private final UsuarioRepository usuarioRepository;

    public InscricaoService(CampeonatoRepository campeonatoRepository,
                             TimeRepository timeRepository,
                             InscricaoRepository inscricaoRepository,
                             DomainEventWriter domainEventWriter,
                             CampeonatoAdminRepository campeonatoAdminRepository,
                             UsuarioRepository usuarioRepository) {
        this.campeonatoRepository = campeonatoRepository;
        this.timeRepository = timeRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.domainEventWriter = domainEventWriter;
        this.campeonatoAdminRepository = campeonatoAdminRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Quem cria é o dono; aprovacaoInscricoes define se capitães entram direto ou aguardam. */
    @Transactional
    public Campeonato criarCampeonato(String nome, CampeonatoFormato formato, UUID donoId,
                                       boolean aprovacaoInscricoes) {
        Campeonato campeonato = campeonatoRepository.save(
                Campeonato.criar(nome, formato, donoId, aprovacaoInscricoes));
        emitirPermissoes(campeonato);
        return campeonato;
    }

    @Transactional(readOnly = true)
    public List<Campeonato> listarCampeonatos() {
        return campeonatoRepository.findAll();
    }

    /** Dono ou admin delegado do campeonato (campeonato legado sem dono: qualquer conta). */
    @Transactional(readOnly = true)
    public boolean podeGerenciar(Campeonato campeonato, UUID usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        return campeonato.ehDono(usuarioId)
                || campeonatoAdminRepository.existsById(new CampeonatoAdminId(campeonato.getId(), usuarioId));
    }

    /** ABERTO/SORTEADO -> SORTEADO: o dashboard chama após gerar os confrontos no partidas-service. */
    @Transactional
    public Campeonato marcarSorteado(UUID campeonatoId, UUID usuarioId) {
        Campeonato campeonato = buscarComPermissao(campeonatoId, usuarioId);
        campeonato.sortear();
        return campeonatoRepository.save(campeonato);
    }

    /** SORTEADO -> ABERTO: reabre inscrições (o sorteio é descartado no partidas-service). */
    @Transactional
    public Campeonato reabrirInscricoes(UUID campeonatoId, UUID usuarioId) {
        Campeonato campeonato = buscarComPermissao(campeonatoId, usuarioId);
        campeonato.reabrirInscricoes();
        return campeonatoRepository.save(campeonato);
    }

    /** SORTEADO -> EM_ANDAMENTO: trava inscrições e chaveamento. */
    @Transactional
    public Campeonato iniciarCampeonato(UUID campeonatoId, UUID usuarioId) {
        Campeonato campeonato = buscarComPermissao(campeonatoId, usuarioId);
        campeonato.iniciar();
        return campeonatoRepository.save(campeonato);
    }

    /** Delegação: só o dono adiciona administradores, identificados pelo email da conta. */
    @Transactional
    public Usuario adicionarAdmin(UUID campeonatoId, UUID solicitanteId, String emailDoAdmin) {
        Campeonato campeonato = buscar(campeonatoId);
        if (!campeonato.ehDono(solicitanteId)) {
            throw new SemPermissaoException("apenas o dono do torneio pode delegar administradores");
        }
        Usuario admin = usuarioRepository.findByEmail(emailDoAdmin == null ? "" : emailDoAdmin.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "nenhuma conta com o email informado — a pessoa precisa se cadastrar primeiro"));
        if (admin.getId().equals(campeonato.getDonoId())) {
            throw new IllegalArgumentException("essa conta ja e dona do torneio");
        }
        campeonatoAdminRepository.save(new CampeonatoAdmin(campeonatoId, admin.getId()));
        emitirPermissoes(campeonato);
        return admin;
    }

    /** Só o dono remove administradores delegados. */
    @Transactional
    public void removerAdmin(UUID campeonatoId, UUID solicitanteId, UUID adminId) {
        Campeonato campeonato = buscar(campeonatoId);
        if (!campeonato.ehDono(solicitanteId)) {
            throw new SemPermissaoException("apenas o dono do torneio pode remover administradores");
        }
        campeonatoAdminRepository.deleteById(new CampeonatoAdminId(campeonatoId, adminId));
        emitirPermissoes(campeonato);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarAdmins(UUID campeonatoId) {
        List<UUID> ids = campeonatoAdminRepository.findByIdCampeonatoId(campeonatoId).stream()
                .map(admin -> admin.getId().getUsuarioId())
                .toList();
        return usuarioRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public List<Inscricao> listarInscricoes(UUID campeonatoId) {
        return inscricaoRepository.findDetalhadoByCampeonatoId(campeonatoId);
    }

    /**
     * Inscreve um time no campeonato. Organizador (dono/admin): dispara a saga
     * na hora (team.registered.v1 no outbox, mesma transação — auto-confirma).
     * Qualquer outro usuário logado: auto-inscrição de capitão, fica PENDENTE
     * até o organizador aprovar (o evento só é gravado na aprovação).
     */
    @Transactional
    public Inscricao inscreverTime(UUID campeonatoId, UUID usuarioId, String nomeTime, List<String> nomesJogadores) {
        Campeonato campeonato = buscar(campeonatoId);
        if (usuarioId == null) {
            throw new NaoAutenticadoException("entre na sua conta para inscrever um time");
        }
        if (!campeonato.aceitaInscricoes()) {
            throw new IllegalStateException("campeonato nao aceita inscricoes: " + campeonatoId);
        }
        if (inscricaoRepository.existsAtivaByCampeonatoIdAndNomeTime(campeonatoId, nomeTime)) {
            throw new IllegalStateException(
                    "time '" + nomeTime + "' ja esta inscrito neste campeonato — use o time existente ao agendar partidas");
        }

        boolean gestor = podeGerenciar(campeonato, usuarioId);
        if (!gestor && inscricaoRepository.existsPendenteDoCapitao(campeonatoId, usuarioId)) {
            throw new IllegalStateException(
                    "voce ja tem uma inscricao aguardando aprovacao neste torneio — cancele-a para tentar com outro time");
        }

        Time time = timeRepository.save(Time.criar(nomeTime, nomesJogadores, usuarioId));
        if (gestor) {
            Inscricao inscricao = inscricaoRepository.save(Inscricao.pendente(time, campeonato));
            escreverTeamRegistered(time, campeonato);
            return inscricao;
        }
        Inscricao inscricao = inscricaoRepository.save(Inscricao.pendenteDeCapitao(time, campeonato, usuarioId));
        if (!campeonato.exigeAprovacaoDeInscricoes()) {
            // modo direto: confirma na hora via saga; o gestor modera removendo
            // times enquanto as inscrições estiverem abertas
            escreverTeamRegistered(time, campeonato);
        }
        return inscricao;
    }

    /** Aprovação do organizador: dispara a saga de confirmação (team.registered.v1). */
    @Transactional
    public Inscricao aprovarInscricao(UUID campeonatoId, UUID inscricaoId, UUID usuarioId) {
        buscarComPermissao(campeonatoId, usuarioId);
        Inscricao inscricao = buscarInscricao(campeonatoId, inscricaoId);
        if (inscricao.getStatus() != InscricaoStatus.PENDENTE) {
            throw new IllegalStateException("so inscricoes PENDENTES podem ser aprovadas: " + inscricao.getStatus());
        }
        escreverTeamRegistered(inscricao.getTime(), inscricao.getCampeonato());
        return inscricao;
    }

    /** Recusa do organizador: o capitão pode se inscrever de novo com outro time. */
    @Transactional
    public Inscricao recusarInscricao(UUID campeonatoId, UUID inscricaoId, UUID usuarioId) {
        buscarComPermissao(campeonatoId, usuarioId);
        Inscricao inscricao = buscarInscricao(campeonatoId, inscricaoId);
        inscricao.recusar();
        return inscricaoRepository.save(inscricao);
    }

    /**
     * Remove uma inscrição: o gestor modera enquanto as inscrições estão
     * abertas (qualquer status — é a moderação do modo sem aprovação); o
     * capitão cancela a própria enquanto PENDENTE.
     */
    @Transactional
    public void removerInscricao(UUID campeonatoId, UUID inscricaoId, UUID usuarioId) {
        Inscricao inscricao = buscarInscricao(campeonatoId, inscricaoId);
        Campeonato campeonato = inscricao.getCampeonato();
        boolean gestor = podeGerenciar(campeonato, usuarioId);
        boolean capitao = usuarioId != null && usuarioId.equals(inscricao.getCapitaoUsuarioId());

        if (gestor) {
            if (!campeonato.aceitaInscricoes()) {
                throw new IllegalStateException(
                        "times so podem ser removidos com as inscricoes abertas — reabra as inscricoes primeiro");
            }
        } else if (capitao) {
            if (inscricao.getStatus() != InscricaoStatus.PENDENTE) {
                throw new IllegalStateException(
                        "so inscricoes PENDENTES podem ser canceladas: " + inscricao.getStatus());
            }
        } else {
            throw new SemPermissaoException("apenas o capitao da inscricao ou um gestor do torneio pode remove-la");
        }

        Time time = inscricao.getTime();
        inscricaoRepository.delete(inscricao);
        timeRepository.delete(time);
    }

    /**
     * Sugestões de reuso ("Meus times"): sempre snapshot — reusar copia nome e
     * elenco para um time novo; editar depois não altera o torneio antigo.
     * Dedupe por nome (case-insensitive), o cadastro mais recente vence.
     */
    @Transactional(readOnly = true)
    public List<TimeReutilizavel> listarTimesReutilizaveis(UUID usuarioId) {
        java.util.Map<String, TimeReutilizavel> porNome = new java.util.LinkedHashMap<>();
        for (Time time : timeRepository.findReutilizaveisPor(usuarioId)) {
            String chave = time.getNome().toLowerCase();
            porNome.putIfAbsent(chave, new TimeReutilizavel(
                    time.getNome(),
                    time.getJogadores().stream().map(jogador -> jogador.getNome()).toList()));
        }
        return List.copyOf(porNome.values());
    }

    public record TimeReutilizavel(String nome, List<String> jogadores) {
    }

    private void escreverTeamRegistered(Time time, Campeonato campeonato) {
        List<PlayerRef> players = time.getJogadores().stream()
                .map(jogador -> new PlayerRef(jogador.getId(), jogador.getNome()))
                .toList();
        domainEventWriter.write(time.getId(), TeamRegisteredPayload.TYPE,
                new TeamRegisteredPayload(time.getId(), time.getNome(), campeonato.getId(), players));
    }

    /**
     * Snapshot de quem gerencia o campeonato — projeção consumida pelo
     * partidas-service para autorizar escrita em partidas (sem chamada
     * síncrona). Legado sem dono não emite: ausência de projeção = regra
     * legada (qualquer logado) do lado de lá.
     */
    private void emitirPermissoes(Campeonato campeonato) {
        if (campeonato.getDonoId() == null) {
            return;
        }
        List<UUID> adminIds = campeonatoAdminRepository.findByIdCampeonatoId(campeonato.getId()).stream()
                .map(admin -> admin.getId().getUsuarioId())
                .toList();
        domainEventWriter.write(campeonato.getId(), ChampionshipPermissionsChangedPayload.TYPE,
                new ChampionshipPermissionsChangedPayload(campeonato.getId(), campeonato.getDonoId(), adminIds));
    }

    private Inscricao buscarInscricao(UUID campeonatoId, UUID inscricaoId) {
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new IllegalArgumentException("inscricao nao encontrada: " + inscricaoId));
        if (!inscricao.getCampeonato().getId().equals(campeonatoId)) {
            throw new IllegalArgumentException("inscricao nao pertence a este campeonato");
        }
        return inscricao;
    }

    private Campeonato buscarComPermissao(UUID campeonatoId, UUID usuarioId) {
        Campeonato campeonato = buscar(campeonatoId);
        if (!podeGerenciar(campeonato, usuarioId)) {
            throw new SemPermissaoException("apenas o dono ou um administrador do torneio pode fazer isso");
        }
        return campeonato;
    }

    private Campeonato buscar(UUID campeonatoId) {
        return campeonatoRepository.findById(campeonatoId)
                .orElseThrow(() -> new IllegalArgumentException("campeonato nao encontrado: " + campeonatoId));
    }
}
