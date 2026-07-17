-- Escolha do organizador na criação do torneio: inscrições de capitães
-- precisam de aprovação (padrão) ou entram confirmadas direto. No modo
-- direto a moderação é a posteriori: o gestor remove times enquanto ABERTO.
ALTER TABLE campeonato ADD COLUMN aprovacao_inscricoes BOOLEAN NOT NULL DEFAULT TRUE;
