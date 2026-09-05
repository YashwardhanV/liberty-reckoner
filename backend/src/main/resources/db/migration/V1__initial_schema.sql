CREATE TABLE prison (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    district VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity >= 0),
    current_population INTEGER NOT NULL CHECK (current_population >= 0)
);

CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE prisoner (
    id UUID PRIMARY KEY,
    prison_number VARCHAR(40) NOT NULL UNIQUE,
    full_name VARCHAR(160) NOT NULL,
    gender VARCHAR(30) NOT NULL,
    date_of_birth DATE,
    nationality VARCHAR(80) NOT NULL,
    preferred_language VARCHAR(80) NOT NULL,
    previous_convictions INTEGER NOT NULL DEFAULT 0 CHECK (previous_convictions >= 0),
    conviction_history_verified BOOLEAN NOT NULL DEFAULT FALSE,
    prison_id UUID NOT NULL REFERENCES prison(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE legal_case (
    id UUID PRIMARY KEY,
    cnr_number VARCHAR(40) NOT NULL UNIQUE,
    fir_number VARCHAR(60) NOT NULL,
    police_station VARCHAR(160) NOT NULL,
    court_name VARCHAR(200) NOT NULL,
    district VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    stage VARCHAR(30) NOT NULL,
    charge_sheet_date DATE,
    next_hearing_date DATE
);

CREATE TABLE prisoner_case (
    id UUID PRIMARY KEY,
    prisoner_id UUID NOT NULL REFERENCES prisoner(id),
    legal_case_id UUID NOT NULL REFERENCES legal_case(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    accused_delay_days INTEGER NOT NULL DEFAULT 0 CHECK (accused_delay_days >= 0),
    accused_delay_verified BOOLEAN NOT NULL DEFAULT TRUE,
    bail_granted_date DATE,
    physical_release_date DATE,
    CONSTRAINT uq_prisoner_case UNIQUE (prisoner_id, legal_case_id)
);

CREATE TABLE charge (
    id UUID PRIMARY KEY,
    prisoner_case_id UUID NOT NULL REFERENCES prisoner_case(id) ON DELETE CASCADE,
    act_name VARCHAR(180) NOT NULL,
    section_code VARCHAR(40) NOT NULL,
    description VARCHAR(240) NOT NULL,
    maximum_term_days INTEGER CHECK (maximum_term_days > 0),
    maximum_term_label VARCHAR(80),
    death_punishment_possible BOOLEAN NOT NULL DEFAULT FALSE,
    life_imprisonment_possible BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL,
    legal_source VARCHAR(80) NOT NULL
);

CREATE TABLE custody_period (
    id UUID PRIMARY KEY,
    prisoner_case_id UUID NOT NULL REFERENCES prisoner_case(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE,
    included BOOLEAN NOT NULL DEFAULT TRUE,
    exclusion_reason VARCHAR(240),
    source_system VARCHAR(60) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_custody_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE legal_assessment (
    id UUID PRIMARY KEY,
    prisoner_case_id UUID NOT NULL REFERENCES prisoner_case(id) ON DELETE CASCADE,
    assessed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(40) NOT NULL,
    verification_status VARCHAR(30) NOT NULL,
    maximum_term_days INTEGER,
    threshold_days INTEGER,
    credited_custody_days INTEGER NOT NULL,
    excluded_delay_days INTEGER NOT NULL,
    projected_threshold_date DATE,
    days_remaining INTEGER,
    threshold_fraction VARCHAR(30) NOT NULL,
    rule_version VARCHAR(40) NOT NULL,
    explanation TEXT NOT NULL,
    blockers_json TEXT NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE workflow_task (
    id UUID PRIMARY KEY,
    prisoner_case_id UUID NOT NULL REFERENCES prisoner_case(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    assigned_role VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    title VARCHAR(220) NOT NULL,
    notes TEXT,
    due_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    actor VARCHAR(180) NOT NULL,
    details_json TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prisoner_name ON prisoner (LOWER(full_name));
CREATE INDEX idx_prisoner_prison ON prisoner (prison_id);
CREATE INDEX idx_case_stage_hearing ON legal_case (stage, next_hearing_date);
CREATE INDEX idx_prisoner_case_active ON prisoner_case (prisoner_id, active);
CREATE INDEX idx_charge_prisoner_case ON charge (prisoner_case_id, active);
CREATE INDEX idx_custody_prisoner_case ON custody_period (prisoner_case_id, start_date);
CREATE INDEX idx_assessment_current_status ON legal_assessment (is_current, status);
CREATE INDEX idx_assessment_case_time ON legal_assessment (prisoner_case_id, assessed_at DESC);
CREATE INDEX idx_task_status_due ON workflow_task (status, due_at);
CREATE INDEX idx_audit_aggregate ON audit_event (aggregate_type, aggregate_id, occurred_at DESC);

