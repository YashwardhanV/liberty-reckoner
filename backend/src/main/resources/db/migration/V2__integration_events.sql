CREATE TABLE integration_event (
    id UUID PRIMARY KEY,
    source_system VARCHAR(80) NOT NULL,
    source_event_id VARCHAR(120) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    prisoner_case_id UUID NOT NULL REFERENCES prisoner_case(id),
    CONSTRAINT uq_integration_source_event UNIQUE (source_system, source_event_id)
);

CREATE INDEX idx_integration_received ON integration_event (received_at DESC);

