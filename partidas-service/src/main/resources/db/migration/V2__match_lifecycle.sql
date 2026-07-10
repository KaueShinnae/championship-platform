-- Ciclo de vida completo da partida (SPEC.md §2):
-- AGENDADA (scheduled_at = horario marcado) -> EM_ANDAMENTO (started_at) -> FINALIZADA (played_at)
ALTER TABLE partida ADD COLUMN started_at TIMESTAMPTZ NULL;
