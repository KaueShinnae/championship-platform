package com.championship.partidas.api;

import com.championship.partidas.api.MatchDtos.AgendarPartidaRequest;
import com.championship.partidas.api.MatchDtos.PartidaResponse;
import com.championship.partidas.api.MatchDtos.RegistrarResultadoRequest;
import com.championship.partidas.application.PartidaService;
import com.championship.partidas.domain.Partida;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final PartidaService partidaService;

    public MatchController(PartidaService partidaService) {
        this.partidaService = partidaService;
    }

    @PostMapping
    public ResponseEntity<PartidaResponse> agendar(@Valid @RequestBody AgendarPartidaRequest request) {
        Partida partida = partidaService.agendar(
                request.championshipId(), request.groupId(),
                request.homeTeamId(), request.homeTeamName(),
                request.awayTeamId(), request.awayTeamName(),
                request.scheduledAt());
        return ResponseEntity.status(201).body(PartidaResponse.from(partida));
    }

    @PostMapping("/{matchId}/start")
    public PartidaResponse iniciar(@PathVariable UUID matchId) {
        return PartidaResponse.from(partidaService.iniciar(matchId));
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
                                               @Valid @RequestBody RegistrarResultadoRequest request) {
        Partida partida = partidaService.registrarResultado(matchId, request.homeScore(), request.awayScore());
        return PartidaResponse.from(partida);
    }
}
