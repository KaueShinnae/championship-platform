-- Opções de criação do torneio (torneios em geral):
-- tamanho de equipe (mín./máx. de integrantes; nulo = sem restrição; torneio
-- individual pode ter mín. 1) e disputa de 3º lugar no mata-mata.
ALTER TABLE campeonato ADD COLUMN min_integrantes INT;
ALTER TABLE campeonato ADD COLUMN max_integrantes INT;
ALTER TABLE campeonato ADD COLUMN disputa_terceiro BOOLEAN NOT NULL DEFAULT FALSE;
