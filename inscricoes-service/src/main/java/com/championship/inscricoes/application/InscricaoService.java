package com.championship.inscricoes.application;

import com.championship.inscricoes.domain.Campeonato;
import com.championship.inscricoes.domain.CampeonatoAdmin;
import com.championship.inscricoes.domain.CampeonatoAdmin.CampeonatoAdminId;
import com.championship.inscricoes.domain.CampeonatoFormato;
import com.championship.inscricoes.domain.GestaoLog;
import com.championship.inscricoes.domain.Inscricao;
import com.championship.inscricoes.domain.InscricaoStatus;
import com.championship.inscricoes.domain.Time;
import com.championship.inscricoes.domain.Usuario;
import com.championship.inscricoes.infrastructure.messaging.DomainEventWriter;
import com.championship.inscricoes.infrastructure.messaging.events.ChampionshipCancelledPayload;
import com.championship.inscricoes.infrastructure.messaging.events.ChampionshipPermissionsChangedPayload;
import com.championship.inscricoes.infrastructure.messaging.events.PlayerRef;
import com.championship.inscricoes.infrastructure.messaging.events.TeamRegisteredPayload;
import com.championship.inscricoes.infrastructure.persistence.CampeonatoAdminRepository;
import com.championship.inscricoes.infrastructure.persistence.CampeonatoRepository;
import com.championship.inscricoes.infrastructure.persistence.GestaoLogRepository;
import com.championship.inscricoes.infrastructure.persistence.InscricaoRepository;
import com.championship.inscricoes.infrastructure.persistence.TimeRepository;
import com.championship.inscricoes.infrastructure.persistence.UsuarioRepository;
import com.championship.inscricoes.infrastructure.security.AuthTokens.Sessao;
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
    private final GestaoLogRepository gestaoLogRepository;

    public InscricaoService(CampeonatoRepository campeonatoRepository,
                             TimeRepository timeRepository,
                             InscricaoRepository inscricaoRepository,
                             DomainEventWriter domainEventWriter,
                             CampeonatoAdminRepository campeonatoAdminRepository,
                             UsuarioRepository usuarioRepository,
                             GestaoLogRepository gestaoLogRepository) {
        this.campeonatoRepository = campeonatoRepository;
        this.timeRepository = timeRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.domainEventWriter = domainEventWriter;
        this.campeonatoAdminRepository = campeonatoAdminRepository;
        this.usuarioRepository = usuarioRepository;
        this.gestaoLogRepository = gestaoLogRepository;
    }

    @Transactional
    public Campeonato criarCampeonato(String nome, CampeonatoFormato formato, UUID donoId,
                                       boolean aprovacaoInscricoes, Integer minIntegrantes, Integer maxIntegrantes,
                                       boolean disputaTerceiro) {
        Campeonato campeonato = campeonatoRepository.save(Campeonato.criar(
                nome, formato, donoId, aprovacaoInscricoes, minIntegrantes, maxIntegrantes, disputaTerceiro));
        emitirPermissoes(campeonato);
        return campeonato;
    }

    @Transactional
    public Campeonato criarCampeonato(String nome, CampeonatoFormato formato, UUID donoId,
                                       boolean aprovacaoInscricoes) {
        return criarCampeonato(nome, formato, donoId, aprovacaoInscricoes, null, null, false);
    }

    @Transactional
    public Campeonato editarCampeonato(UUID campeonatoId, UUID usuarioId, String nome, boolean aprovacaoInscricoes,
                                        Integer minIntegrantes, Integer maxIntegrantes, boolean disputaTerceiro) {
        Campeonato campeonato = buscarComPermissao(campeonatoId, usuarioId);
        campeonato.editar(nome, aprovacaoInscricoes, minIntegrantes, maxIntegrantes, disputaTerceiro);
        return campeonatoRepository.save(campeonato);
    }

    @Transactional
    public Campeonato cancelarCampeonato(UUID campeonatoId, UUID usuarioId) {
        Campeonato campeonato = buscar(campeonatoId);
        if (!campeonato.ehDono(usuarioId)) {
            throw new SemPermissaoException("apenas o dono do torneio pode cancelá-lo");
        }
        campeonato.cancelar();
        campeonatoRepository.save(campeonato);
        domainEventWriter.write(campeonato.getId(), ChampionshipCancelledPayload.TYPE,
                new ChampionshipCancelledPayload(campeonato.getId()));
        return campeonato;
    }

    @Transactional(readOnly = true)
    public List<Campeonato> listarCampeonatos() {
        return campeonatoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean podeGerenciar(Campeonato campeonato, UUID usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        return campeonato.ehDono(usuarioId)
                || campeonatoAdminRepository.existsById(new CampeonatoAdminId(campeonato.getId(), usuarioId));
    }

    @Transactional
    public Campeonato marcarSorteado(UUID campeonatoId, UUID usuarioId) {
        Campeonato campeonato = buscarComPermissao(campeonatoId, usuarioId);
        campeonato.sortear();
        return campeonatoRepository.save(campeonato);
    }

    @Transactional
    public Campeonato reabrirInscricoes(UUID campeonatoId, UUID usuarioId) {
        Campeonato campeonato = buscarComPermissao(campeonatoId, usuarioId);
        campeonato.reabrirInscricoes();
        return campeonatoRepository.save(campeonato);
    }

    @Transactional
    public Campeonato iniciarCampeonato(UUID campeonatoId, UUID usuarioId) {
        Campeonato campeonato = buscarComPermissao(campeonatoId, usuarioId);
        campeonato.iniciar();
        return campeonatoRepository.save(campeonato);
    }

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
        campeonato.validarNumeroDeIntegrantes(nomesJogadores == null ? 0 : nomesJogadores.size());

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

    @Transactional
    public Inscricao aprovarInscricao(UUID campeonatoId, UUID inscricaoId, Sessao ator) {
        buscarComPermissao(campeonatoId, ator.id());
        Inscricao inscricao = buscarInscricao(campeonatoId, inscricaoId);
        if (inscricao.getStatus() != InscricaoStatus.PENDENTE) {
            throw new IllegalStateException("so inscricoes PENDENTES podem ser aprovadas: " + inscricao.getStatus());
        }
        escreverTeamRegistered(inscricao.getTime(), inscricao.getCampeonato());
        registrarLog(campeonatoId, ator, "APROVACAO", "aprovou a inscrição de \"" + inscricao.getTime().getNome() + "\"");
        return inscricao;
    }

    @Transactional
    public Inscricao recusarInscricao(UUID campeonatoId, UUID inscricaoId, Sessao ator) {
        buscarComPermissao(campeonatoId, ator.id());
        Inscricao inscricao = buscarInscricao(campeonatoId, inscricaoId);
        inscricao.recusar();
        inscricaoRepository.save(inscricao);
        registrarLog(campeonatoId, ator, "RECUSA", "recusou a inscrição de \"" + inscricao.getTime().getNome() + "\"");
        return inscricao;
    }

    @Transactional
    public void removerInscricao(UUID campeonatoId, UUID inscricaoId, Sessao ator) {
        Inscricao inscricao = buscarInscricao(campeonatoId, inscricaoId);
        Campeonato campeonato = inscricao.getCampeonato();
        boolean gestor = podeGerenciar(campeonato, ator.id());
        boolean capitao = ator.id() != null && ator.id().equals(inscricao.getCapitaoUsuarioId());

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
        String nomeTime = time.getNome();
        inscricaoRepository.delete(inscricao);
        timeRepository.delete(time);
        if (gestor) {
            registrarLog(campeonatoId, ator, "REMOCAO", "removeu o time \"" + nomeTime + "\"");
        }
    }

    @Transactional
    public Inscricao editarTime(UUID campeonatoId, UUID inscricaoId, String novoNome,
                                 List<String> nomesJogadores, Sessao ator) {
        Campeonato campeonato = buscarComPermissao(campeonatoId, ator.id());
        if (!campeonato.aceitaInscricoes()) {
            throw new IllegalStateException(
                    "times só podem ser editados com as inscrições abertas — reabra as inscrições primeiro");
        }
        Inscricao inscricao = buscarInscricao(campeonatoId, inscricaoId);
        String nomeAntigo = inscricao.getTime().getNome();
        if (!nomeAntigo.equalsIgnoreCase(novoNome)
                && inscricaoRepository.existsAtivaByCampeonatoIdAndNomeTime(campeonatoId, novoNome)) {
            throw new IllegalStateException("já existe um time com esse nome neste torneio");
        }
        campeonato.validarNumeroDeIntegrantes(nomesJogadores == null ? 0 : nomesJogadores.size());
        inscricao.getTime().editar(novoNome, nomesJogadores);
        timeRepository.save(inscricao.getTime());
        String descricao = nomeAntigo.equals(novoNome)
                ? "editou o elenco de \"" + novoNome + "\""
                : "renomeou \"" + nomeAntigo + "\" para \"" + novoNome + "\"";
        registrarLog(campeonatoId, ator, "EDICAO_TIME", descricao);
        return inscricao;
    }

    @Transactional(readOnly = true)
    public List<GestaoLog> listarGestaoLog(UUID campeonatoId) {
        return gestaoLogRepository.findByCampeonatoIdOrderByCreatedAtDesc(campeonatoId);
    }

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

    private void registrarLog(UUID campeonatoId, Sessao ator, String acao, String descricao) {
        gestaoLogRepository.save(new GestaoLog(campeonatoId, ator.id(), ator.nome(), acao, descricao));
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
