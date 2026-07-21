package com.championship.partidas.api;

import com.championship.partidas.api.MatchDtos.AgendarPartidaRequest;
import com.championship.partidas.api.MatchDtos.ChaveSlotResponse;
import com.championship.partidas.api.MatchDtos.ConflitoHorarioResponse;
import com.championship.partidas.api.MatchDtos.CorrigirResultadoRequest;
import com.championship.partidas.api.MatchDtos.DesistenciaRequest;
import com.championship.partidas.api.MatchDtos.GerarConfrontosRequest;
import com.championship.partidas.api.MatchDtos.GestaoLogResponse;
import com.championship.partidas.api.MatchDtos.GroupStandingsResponse;
import com.championship.partidas.api.MatchDtos.PartidaResponse;
import com.championship.partidas.api.MatchDtos.ReagendarEmLoteRequest;
import com.championship.partidas.api.MatchDtos.ReagendarPartidaRequest;
import com.championship.partidas.api.MatchDtos.RegistrarResultadoRequest;
import com.championship.partidas.api.MatchDtos.StandingEntryResponse;
import com.championship.partidas.application.AutorizacaoService;
import com.championship.partidas.application.ChaveamentoService;
import com.championship.partidas.application.PartidaService;
import com.championship.partidas.domain.Partida;
import com.championship.partidas.infrastructure.messaging.events.TeamRef;
import com.championship.partidas.infrastructure.security.AuthTokens;
import com.championship.partidas.infrastructure.security.AuthTokens.Sessao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final PartidaService partidaService;
    private final ChaveamentoService chaveamentoService;
    private final AuthTokens authTokens;
    private final AutorizacaoService autorizacaoService;

    public MatchController(PartidaService partidaService, ChaveamentoService chaveamentoService,
                            AuthTokens authTokens, AutorizacaoService autorizacaoService) {
        this.partidaService = partidaService;
        this.chaveamentoService = chaveamentoService;
        this.authTokens = authTokens;
        this.autorizacaoService = autorizacaoService;
    }

    @PostMapping
    public ResponseEntity<PartidaResponse> agendar(@Valid @RequestBody AgendarPartidaRequest request,
                                                    HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        autorizacaoService.exigirGestor(request.championshipId(), usuarioId);
        Partida partida = partidaService.agendar(
                request.championshipId(), request.groupId(),
                request.homeTeamId(), request.homeTeamName(),
                request.awayTeamId(), request.awayTeamName(),
                request.scheduledAt(), request.local());
        return ResponseEntity.status(201).body(PartidaResponse.from(partida));
    }

    @PostMapping("/generate")
    public ResponseEntity<List<PartidaResponse>> gerarConfrontos(@Valid @RequestBody GerarConfrontosRequest request,
                                                                  HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        autorizacaoService.exigirGestor(request.championshipId(), usuarioId);
        List<TeamRef> times = request.teams().stream()
                .map(time -> new TeamRef(time.teamId(), time.name()))
                .toList();
        boolean terceiro = request.disputaTerceiro() != null && request.disputaTerceiro();
        List<Partida> criadas = chaveamentoService.gerar(request.championshipId(), request.formato(), times, terceiro);
        return ResponseEntity.status(201).body(criadas.stream().map(PartidaResponse::from).toList());
    }

    @GetMapping("/draw/{championshipId}/slots")
    public List<ChaveSlotResponse> slotsDoBracket(@PathVariable UUID championshipId) {
        return chaveamentoService.listarSlots(championshipId).stream()
                .map(slot -> new ChaveSlotResponse(
                        slot.getId().getRound(), slot.getId().getSlot(), slot.getTeamId(), slot.getTeamName()))
                .toList();
    }

    @DeleteMapping("/draw/{championshipId}")
    public ResponseEntity<Void> descartarSorteio(@PathVariable UUID championshipId, HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        autorizacaoService.exigirGestor(championshipId, usuarioId);
        chaveamentoService.descartarSorteio(championshipId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{matchId}/schedule")
    public PartidaResponse reagendar(@PathVariable UUID matchId,
                                      @Valid @RequestBody ReagendarPartidaRequest request,
                                      HttpServletRequest http) {
        exigirGestorDaPartida(matchId, http);
        return PartidaResponse.from(partidaService.reagendar(matchId, request.scheduledAt(), request.local()));
    }

    @PostMapping("/{matchId}/start")
    public PartidaResponse iniciar(@PathVariable UUID matchId, HttpServletRequest http) {
        exigirGestorDaPartida(matchId, http);
        return PartidaResponse.from(partidaService.iniciar(matchId));
    }

    @PostMapping("/{matchId}/score")
    public PartidaResponse atualizarPlacar(@PathVariable UUID matchId,
                                            @Valid @RequestBody RegistrarResultadoRequest request,
                                            HttpServletRequest http) {
        exigirGestorDaPartida(matchId, http);
        return PartidaResponse.from(partidaService.atualizarPlacar(matchId, request.homeScore(), request.awayScore()));
    }

    @GetMapping
    public List<PartidaResponse> listar(@RequestParam(name = "group_id", required = false) UUID groupId) {
        return partidaService.listar(groupId).stream().map(PartidaResponse::from).toList();
    }

    @GetMapping("/{matchId}")
    public PartidaResponse buscar(@PathVariable UUID matchId) {
        return PartidaResponse.from(partidaService.buscar(matchId));
    }

    @PostMapping("/{matchId}/result")
    public PartidaResponse registrarResultado(@PathVariable UUID matchId,
                                               @Valid @RequestBody RegistrarResultadoRequest request,
                                               HttpServletRequest http) {
        Sessao ator = exigirGestorDaPartida(matchId, http);
        boolean wo = request.wo() != null && request.wo();
        Partida partida = partidaService.registrarResultado(
                matchId, request.homeScore(), request.awayScore(), wo, request.woMotivo(), ator);
        return PartidaResponse.from(partida);
    }

    @PostMapping("/{matchId}/correct")
    public PartidaResponse corrigirResultado(@PathVariable UUID matchId,
                                              @Valid @RequestBody CorrigirResultadoRequest request,
                                              HttpServletRequest http) {
        Sessao ator = exigirGestorDaPartida(matchId, http);
        Partida partida = partidaService.corrigirResultado(matchId, request.homeScore(), request.awayScore(), ator);
        return PartidaResponse.from(partida);
    }

    @PostMapping("/reschedule-batch")
    public ResponseEntity<java.util.Map<String, Integer>> reagendarEmLote(
            @Valid @RequestBody ReagendarEmLoteRequest request, HttpServletRequest http) {
        Sessao ator = authTokens.sessao(http);
        autorizacaoService.exigirGestor(request.championshipId(), ator.id());
        int afetadas = partidaService.reagendarEmLote(request.championshipId(), request.shiftMinutes(), ator);
        return ResponseEntity.ok(java.util.Map.of("rescheduled", afetadas));
    }

    @GetMapping("/conflicts/{championshipId}")
    public List<ConflitoHorarioResponse> conflitos(@PathVariable UUID championshipId) {
        return partidaService.conflitosDeHorario(championshipId).stream()
                .map(c -> new ConflitoHorarioResponse(
                        c.partidaA(), c.partidaB(), c.tipo(), c.teamId(), c.teamName(), c.local(), c.scheduledAt()))
                .toList();
    }

    @GetMapping("/standings/{groupId}")
    public ResponseEntity<GroupStandingsResponse> standings(@PathVariable UUID groupId) {
        List<StandingEntryResponse> linhas = partidaService.classificacaoDoGrupo(groupId).stream()
                .map(s -> new StandingEntryResponse(
                        s.teamId(), s.teamName(), s.pontos(), s.vitorias(), s.empates(), s.derrotas(),
                        s.pro(), s.contra(), s.saldo(), s.desempate()))
                .toList();
        if (linhas.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new GroupStandingsResponse(groupId, linhas));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<java.util.Map<String, Integer>> desistir(
            @Valid @RequestBody DesistenciaRequest request, HttpServletRequest http) {
        Sessao ator = authTokens.sessao(http);
        autorizacaoService.exigirGestor(request.championshipId(), ator.id());
        int afetadas = partidaService.desistirTime(request.championshipId(), request.teamId(), ator);
        return ResponseEntity.ok(java.util.Map.of("walkovers", afetadas));
    }

    @GetMapping("/management-log/{championshipId}")
    public List<GestaoLogResponse> gestaoLog(@PathVariable UUID championshipId) {
        return partidaService.listarGestaoLog(championshipId).stream()
                .map(l -> new GestaoLogResponse(
                        l.getId(), l.getActorId(), l.getActorNome(), l.getAcao(), l.getDescricao(), l.getCreatedAt()))
                .toList();
    }

    private Sessao exigirGestorDaPartida(UUID matchId, HttpServletRequest http) {
        Sessao ator = authTokens.sessao(http);
        Partida partida = partidaService.buscar(matchId);
        autorizacaoService.exigirGestor(partida.getCampeonatoId(), ator.id());
        return ator;
    }
}
