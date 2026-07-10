CREATE TABLE partida (
    id UUID PRIMARY KEY,
    campeonato_id UUID NOT NULL,
    group_id UUID NULL,
    home_team_id UUID NOT NULL,
    home_team_name VARCHAR(100) NOT NULL,
    away_team_id UUID NOT NULL,
    away_team_name VARCHAR(100) NOT NULL,
    home_score INT NULL,
    away_score INT NULL,
    status VARCHAR(20) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    played_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_partida_campeonato_id ON partida (campeonato_id);
CREATE INDEX idx_partida_group_id ON partida (group_id);

-- Transactional outbox (skill kafka-event-design)
CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_outbox_event_unpublished ON outbox_event (created_at) WHERE published = FALSE;
