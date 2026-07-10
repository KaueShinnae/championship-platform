CREATE TABLE campeonato (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE time (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE jogador (
    id UUID PRIMARY KEY,
    time_id UUID NOT NULL REFERENCES time (id),
    nome VARCHAR(100) NOT NULL
);

CREATE INDEX idx_jogador_time_id ON jogador (time_id);

CREATE TABLE inscricao (
    id UUID PRIMARY KEY,
    time_id UUID NOT NULL REFERENCES time (id),
    campeonato_id UUID NOT NULL REFERENCES campeonato (id),
    status VARCHAR(20) NOT NULL,
    group_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_inscricao_time_campeonato UNIQUE (time_id, campeonato_id)
);

CREATE INDEX idx_inscricao_campeonato_id ON inscricao (campeonato_id);

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
