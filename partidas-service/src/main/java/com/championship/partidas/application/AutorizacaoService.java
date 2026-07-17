package com.championship.partidas.application;

import com.championship.partidas.infrastructure.persistence.CampeonatoPermissao.CampeonatoPermissaoId;
import com.championship.partidas.infrastructure.persistence.CampeonatoPermissaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Autorização por papel usando a projeção local de permissões (evento
 * championship.permissions.changed.v1) — nunca chamada síncrona a outro
 * serviço. Fail-closed quando a projeção conhece o campeonato; sem projeção
 * (legado sem dono ou janela de propagação de segundos), qualquer usuário
 * autenticado gerencia — mesma regra de produto do inscricoes-service.
 */
@Service
public class AutorizacaoService {

    private final CampeonatoPermissaoRepository permissaoRepository;

    public AutorizacaoService(CampeonatoPermissaoRepository permissaoRepository) {
        this.permissaoRepository = permissaoRepository;
    }

    @Transactional(readOnly = true)
    public void exigirGestor(UUID campeonatoId, UUID usuarioId) {
        if (!permissaoRepository.existsByIdCampeonatoId(campeonatoId)) {
            return;
        }
        if (!permissaoRepository.existsById(new CampeonatoPermissaoId(campeonatoId, usuarioId))) {
            throw new SemPermissaoException(
                    "apenas o dono ou um administrador deste torneio pode gerenciar as partidas");
        }
    }
}
