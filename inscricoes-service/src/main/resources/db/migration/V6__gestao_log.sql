-- Histórico de gestão em linguagem humana (quem aprovou/recusou/removeu/editou
-- time, quando) — separado do feed técnico do Monitoramento. O nome do ator
-- vem do token da sessão.
CREATE TABLE gestao_log (
    id UUID PRIMARY KEY,
    campeonato_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    actor_nome VARCHAR(100) NOT NULL,
    acao VARCHAR(40) NOT NULL,
    descricao VARCHAR(300) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_gestao_log_campeonato ON gestao_log (campeonato_id, created_at DESC);
