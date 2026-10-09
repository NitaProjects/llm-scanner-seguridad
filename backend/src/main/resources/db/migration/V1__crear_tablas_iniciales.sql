CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(6) NOT NULL,
    CONSTRAINT projects_name_not_blank CHECK (name ~ '[^[:space:]]'),
    CONSTRAINT projects_dates_consistent CHECK (updated_at >= created_at)
);

CREATE TABLE scans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id),
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    global_score NUMERIC(5, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE test_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    payload_template TEXT NOT NULL,
    owasp_reference VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE test_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scan_id UUID NOT NULL REFERENCES scans(id),
    test_case_id UUID NOT NULL REFERENCES test_cases(id),
    request_payload TEXT,
    response_payload TEXT,
    evaluator_result TEXT,
    confidence NUMERIC(5, 2),
    executed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE findings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scan_id UUID NOT NULL REFERENCES scans(id),
    test_case_id UUID REFERENCES test_cases(id),
    test_execution_id UUID REFERENCES test_executions(id),
    risk_level VARCHAR(30) NOT NULL,
    owasp_category VARCHAR(100) NOT NULL,
    evidence TEXT,
    explanation TEXT,
    recommendation TEXT,
    confidence NUMERIC(5, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_scans_project_id ON scans(project_id);
CREATE INDEX idx_test_executions_scan_id ON test_executions(scan_id);
CREATE INDEX idx_findings_scan_id ON findings(scan_id);
