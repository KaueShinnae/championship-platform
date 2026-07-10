package com.championship.inscricoes.api;

import com.championship.inscricoes.api.CampeonatoDtos.CampeonatoResponse;
import com.championship.inscricoes.api.CampeonatoDtos.CriarCampeonatoRequest;
import com.championship.inscricoes.application.InscricaoService;
import com.championship.inscricoes.domain.Campeonato;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/campeonatos")
public class CampeonatoController {

    private final InscricaoService inscricaoService;

    public CampeonatoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    @PostMapping
    public ResponseEntity<CampeonatoResponse> criar(@Valid @RequestBody CriarCampeonatoRequest request) {
        Campeonato campeonato = inscricaoService.criarCampeonato(request.nome());
        return ResponseEntity.status(201).body(CampeonatoResponse.from(campeonato));
    }

    @GetMapping
    public List<CampeonatoResponse> listar() {
        return inscricaoService.listarCampeonatos().stream().map(CampeonatoResponse::from).toList();
    }
}
