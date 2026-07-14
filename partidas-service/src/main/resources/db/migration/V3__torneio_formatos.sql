-- Formatos de torneio (grupos+playoffs, mata-mata, pontos corridos):
-- a partida ganha fase/rodada de chaveamento, e o servico passa a guardar a
-- configuracao do sorteio e os slots do bracket por campeonato.
ALTER TABLE partida ADD COLUMN stage VARCHAR(20) NULL;       -- GRUPOS | PLAYOFF (null = legado)
ALTER TABLE partida ADD COLUMN round INT NULL;               -- rodada do mata-mata (1..total_rounds)
ALTER TABLE partida ADD COLUMN bracket_pos INT NULL;         -- posicao do confronto dentro da rodada

-- Configuracao do sorteio de um campeonato (escrita no "sortear confrontos")
CREATE TABLE torneio_chaveamento (
    campeonato_id UUID PRIMARY KEY,
    formato VARCHAR(20) NOT NULL,          -- GRUPOS_PLAYOFFS | PLAYOFFS | PONTOS_CORRIDOS
    total_rounds INT NULL,                 -- rodadas de mata-mata (null em pontos corridos)
    draw_order JSONB NOT NULL,             -- team_ids na ordem sorteada (ultimo criterio de desempate)
    group_ids JSONB NULL,                  -- group_ids na ordem A, B, C... (null sem fase de grupos)
    created_at TIMESTAMPTZ NOT NULL
);

-- Slots do bracket: um time que "entra" numa rodada ocupa um slot; quando os
-- dois slots de um confronto estao ocupados, a partida e criada. Tambem
-- representa byes (time sem adversario entra direto na rodada seguinte).
CREATE TABLE chave_slot (
    campeonato_id UUID NOT NULL,
    round INT NOT NULL,
    slot INT NOT NULL,
    team_id UUID NOT NULL,
    team_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (campeonato_id, round, slot)
);
