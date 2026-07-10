-- Enriquece o registro de idempotencia para alimentar o feed de eventos do
-- web-dashboard (SPEC.md: evidenciar match.finished.v1 -> ranking.updated.v1).
-- Colunas anulaveis: linhas antigas continuam validas para deduplicacao.
ALTER TABLE processed_events ADD COLUMN event_type VARCHAR(100) NULL;
ALTER TABLE processed_events ADD COLUMN aggregate_id UUID NULL;

CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at DESC);
