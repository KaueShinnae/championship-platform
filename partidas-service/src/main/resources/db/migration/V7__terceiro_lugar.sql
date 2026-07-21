-- Disputa de 3º lugar (opcional no mata-mata): perdedores das semifinais se
-- enfrentam. Universal em torneios com pódio. A flag do torneio fica no
-- chaveamento; a partida do 3º lugar é marcada para não contar como final.
ALTER TABLE torneio_chaveamento ADD COLUMN disputa_terceiro BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE partida ADD COLUMN terceiro_lugar BOOLEAN NOT NULL DEFAULT FALSE;
