-- Projeção local de quem gerencia cada campeonato, alimentada pelo evento
-- championship.permissions.changed.v1 (inscricoes-service). Autoriza escrita
-- em partidas sem chamada síncrona. Campeonato sem linhas = legado sem dono
-- (qualquer usuário autenticado gerencia, até ser reivindicado).
CREATE TABLE campeonato_permissao (
    campeonato_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    papel VARCHAR(10) NOT NULL,
    PRIMARY KEY (campeonato_id, usuario_id)
);

-- Idempotência de consumer (skill kafka-event-design): event_id já visto é ignorado.
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
