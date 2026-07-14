-- Formatos de torneio: o campeonato ganha o formato escolhido na criacao e o
-- registro do campeao (preenchido ao consumir championship.completed.v1).
-- Campeonatos existentes viram PONTOS_CORRIDOS (formato mais proximo do fluxo legado).
ALTER TABLE campeonato ADD COLUMN formato VARCHAR(20) NOT NULL DEFAULT 'PONTOS_CORRIDOS';
ALTER TABLE campeonato ADD COLUMN campeao_time_id UUID NULL;
ALTER TABLE campeonato ADD COLUMN campeao_nome VARCHAR(100) NULL;
