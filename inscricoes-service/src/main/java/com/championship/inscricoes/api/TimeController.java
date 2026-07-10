package com.championship.inscricoes.api;

import com.championship.inscricoes.api.TimeDtos.InscreverTimeRequest;
import com.championship.inscricoes.api.TimeDtos.InscricaoDetalheResponse;
import com.championship.inscricoes.api.TimeDtos.InscricaoResponse;
import com.championship.inscricoes.application.InscricaoService;
import com.championship.inscricoes.domain.Inscricao;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/campeonatos/{campeonatoId}/times")
public class TimeController {

    private final InscricaoService inscricaoService;

    public TimeController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    @PostMapping
    public ResponseEntity<InscricaoResponse> inscrever(@PathVariable UUID campeonatoId,
                                                         @Valid @RequestBody InscreverTimeRequest request) {
        Inscricao inscricao = inscricaoService.inscreverTime(campeonatoId, request.nome(), request.jogadores());
        return ResponseEntity.status(201).body(InscricaoResponse.from(inscricao));
    }

    @GetMapping
    public List<InscricaoDetalheResponse> listar(@PathVariable UUID campeonatoId) {
        return inscricaoService.listarInscricoes(campeonatoId).stream()
                .map(InscricaoDetalheResponse::from)
                .toList();
    }
}
