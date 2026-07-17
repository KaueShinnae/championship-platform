package com.championship.partidas.api;

import com.championship.partidas.api.MatchDtos.AgendarPartidaRequest;
import com.championship.partidas.api.MatchDtos.ChaveSlotResponse;
import com.championship.partidas.api.MatchDtos.GerarConfrontosRequest;
import com.championship.partidas.api.MatchDtos.PartidaResponse;
import com.championship.partidas.api.MatchDtos.ReagendarPartidaRequest;
import com.championship.partidas.api.MatchDtos.RegistrarResultadoRequest;
import com.championship.partidas.application.AutorizacaoService;
import com.championship.partidas.application.ChaveamentoService;
import com.championship.partidas.application.PartidaService;
import com.championship.partidas.domain.Partida;
import com.championship.partidas.infrastructure.messaging.events.TeamRef;
import com.championship.partidas.infrastructure.security.AuthTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Leitura é pública (visitante acompanha sem conta); toda mutação exige
 * sessão válida E papel de gestão no campeonato — checado localmente na
 * projeção alimentada por championship.permissions.changed.v1 (fail-closed
 * quando a projeção conhece o campeonato; ver AutorizacaoService).
 */
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
                request.scheduledAt());
        return ResponseEntity.status(201).body(PartidaResponse.from(partida));
    }

    /** Sorteia os times e gera todos os confrontos do formato (re-sortear regenera). */
    @PostMapping("/generate")
    public ResponseEntity<List<PartidaResponse>> gerarConfrontos(@Valid @RequestBody GerarConfrontosRequest request,
                                                                  HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        autorizacaoService.exigirGestor(request.championshipId(), usuarioId);
        List<TeamRef> times = request.teams().stream()
                .map(time -> new TeamRef(time.teamId(), time.name()))
                .toList();
        List<Partida> criadas = chaveamentoService.gerar(request.championshipId(), request.formato(), times);
        return ResponseEntity.status(201).body(criadas.stream().map(PartidaResponse::from).toList());
    }

    /** Slots ocupados do bracket — o dashboard usa para mostrar byes e quem aguarda adversário. */
    @GetMapping("/draw/{championshipId}/slots")
    public List<ChaveSlotResponse> slotsDoBracket(@PathVariable UUID championshipId) {
        return chaveamentoService.listarSlots(championshipId).stream()
                .map(slot -> new ChaveSlotResponse(
                        slot.getId().getRound(), slot.getId().getSlot(), slot.getTeamId(), slot.getTeamName()))
                .toList();
    }

    /** Descarta o sorteio (reabrir inscrições) — só enquanto nada foi iniciado. */
    @DeleteMapping("/draw/{championshipId}")
    public ResponseEntity<Void> descartarSorteio(@PathVariable UUID championshipId, HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        autorizacaoService.exigirGestor(championshipId, usuarioId);
        chaveamentoService.descartarSorteio(championshipId);
        return ResponseEntity.noContent().build();
    }

    /** Remarca data/horário de uma partida ainda não iniciada. */
    @PostMapping("/{matchId}/schedule")
    public PartidaResponse reagendar(@PathVariable UUID matchId,
                                      @Valid @RequestBody ReagendarPartidaRequest request,
                                      HttpServletRequest http) {
        exigirGestorDaPartida(matchId, http);
        return PartidaResponse.from(partidaService.reagendar(matchId, request.scheduledAt()));
    }

    @PostMapping("/{matchId}/start")
    public PartidaResponse iniciar(@PathVariable UUID matchId, HttpServletRequest http) {
        exigirGestorDaPartida(matchId, http);
        return PartidaResponse.from(partidaService.iniciar(matchId));
    }

    /** Placar parcial ao vivo (contagem do organizador; visitante lê via polling). */
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
        exigirGestorDaPartida(matchId, http);
        Partida partida = partidaService.registrarResultado(matchId, request.homeScore(), request.awayScore());
        return PartidaResponse.from(partida);
    }

    /** Mutações por partida: resolve o campeonato da partida e exige papel de gestão. */
    private void exigirGestorDaPartida(UUID matchId, HttpServletRequest http) {
        UUID usuarioId = authTokens.exigirUsuario(http);
        Partida partida = partidaService.buscar(matchId);
        autorizacaoService.exigirGestor(partida.getCampeonatoId(), usuarioId);
    }
}
