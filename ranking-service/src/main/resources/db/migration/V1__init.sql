-- Read model CQRS: populado exclusivamente pelo consumer de match.finished.v1
CREATE TABLE group_standing (
    id UUID PRIMARY KEY,
    campeonato_id UUID NOT NULL,
    group_id UUID NOT NULL,
    team_id UUID NOT NULL,
    team_name VARCHAR(100) NOT NULL,
    points INT NOT NULL DEFAULT 0,
    wins INT NOT NULL DEFAULT 0,
    draws INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    goals_for INT NOT NULL DEFAULT 0,
    goals_against INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_group_standing_group_team UNIQUE (group_id, team_id)
);

CREATE INDEX idx_group_standing_group_id ON group_standing (group_id);

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

-- Idempotencia de consumer (skill kafka-event-design)
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
