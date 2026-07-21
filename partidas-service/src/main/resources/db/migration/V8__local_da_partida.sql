-- Item de gestão: local/sede da partida (quadra, mesa, tabuleiro…) para
-- torneios presenciais multi-local. Nulo = "a definir" (nunca inventar local).
-- Aditivo: partidas existentes ficam com local nulo.
ALTER TABLE partida ADD COLUMN local VARCHAR(120);
