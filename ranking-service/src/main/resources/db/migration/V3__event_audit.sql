-- Rastreabilidade para a tela de Monitoramento do web-dashboard: guarda o
-- payload bruto do evento consumido e o trace id (OpenTelemetry) tanto no
-- consumo quanto na publicacao. Colunas anulaveis: linhas antigas continuam
-- validas para deduplicacao e para o feed.
ALTER TABLE processed_events ADD COLUMN payload JSONB NULL;
ALTER TABLE processed_events ADD COLUMN trace_id VARCHAR(32) NULL;

ALTER TABLE outbox_event ADD COLUMN trace_id VARCHAR(32) NULL;
