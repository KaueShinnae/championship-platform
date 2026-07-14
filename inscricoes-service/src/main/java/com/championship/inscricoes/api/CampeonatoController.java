package com.championship.inscricoes.api;

import com.championship.inscricoes.api.CampeonatoDtos.CampeonatoResponse;
import com.championship.inscricoes.api.CampeonatoDtos.CriarCampeonatoRequest;
import com.championship.inscricoes.application.InscricaoService;
import com.championship.inscricoes.domain.Campeonato;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/campeonatos")
public class CampeonatoController {

    private final InscricaoService inscricaoService;

    public CampeonatoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    @PostMapping
    public ResponseEntity<CampeonatoResponse> criar(@Valid @RequestBody CriarCampeonatoRequest request) {
        Campeonato campeonato = inscricaoService.criarCampeonato(request.nome(), request.formato());
        return ResponseEntity.status(201).body(CampeonatoResponse.from(campeonato));
    }

    @GetMapping
    public List<CampeonatoResponse> listar() {
        return inscricaoService.listarCampeonatos().stream().map(CampeonatoResponse::from).toList();
    }

    @PostMapping("/{campeonatoId}/sortear")
    public CampeonatoResponse sortear(@PathVariable UUID campeonatoId) {
        return CampeonatoResponse.from(inscricaoService.marcarSorteado(campeonatoId));
    }

    @PostMapping("/{campeonatoId}/reabrir")
    public CampeonatoResponse reabrir(@PathVariable UUID campeonatoId) {
        return CampeonatoResponse.from(inscricaoService.reabrirInscricoes(campeonatoId));
    }

    @PostMapping("/{campeonatoId}/iniciar")
    public CampeonatoResponse iniciar(@PathVariable UUID campeonatoId) {
        return CampeonatoResponse.from(inscricaoService.iniciarCampeonato(campeonatoId));
    }
}
