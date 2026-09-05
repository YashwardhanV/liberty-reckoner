ALTER TABLE prisoner_case ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE workflow_task ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE outbound_notification (
    id UUID PRIMARY KEY,
    event_key VARCHAR(180) NOT NULL UNIQUE,
    prisoner_case_id UUID NOT NULL REFERENCES prisoner_case(id) ON DELETE CASCADE,
    channel VARCHAR(30) NOT NULL,
    recipient_role VARCHAR(40) NOT NULL,
    recipient_address VARCHAR(240) NOT NULL,
    subject VARCHAR(240) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    locked_at TIMESTAMPTZ,
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_notification_dispatch
    ON outbound_notification (status, next_attempt_at, created_at);
CREATE INDEX idx_notification_case
    ON outbound_notification (prisoner_case_id, created_at DESC);
