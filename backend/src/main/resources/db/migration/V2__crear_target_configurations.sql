CREATE TABLE target_configurations (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    name VARCHAR(120),
    url VARCHAR(2048) NOT NULL,
    request_template TEXT NOT NULL,
    response_path VARCHAR(1024) NOT NULL,
    headers JSONB NOT NULL DEFAULT '{}'::jsonb,
    auth_type VARCHAR(20) NOT NULL,
    auth_header_name VARCHAR(128),
    credential_ciphertext BYTEA,
    timeout_ms INTEGER NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL,
    updated_at TIMESTAMPTZ(6) NOT NULL,
    CONSTRAINT uq_target_configurations_project_id UNIQUE (project_id),
    CONSTRAINT ck_target_timeout CHECK (timeout_ms BETWEEN 1000 AND 60000),
    CONSTRAINT ck_target_dates CHECK (updated_at >= created_at),
    CONSTRAINT ck_target_headers_object CHECK (jsonb_typeof(headers) = 'object'),
    CONSTRAINT ck_target_auth CHECK (
        (auth_type = 'NONE' AND auth_header_name IS NULL AND credential_ciphertext IS NULL)
        OR (auth_type = 'BEARER_TOKEN' AND auth_header_name IS NULL AND credential_ciphertext IS NOT NULL)
        OR (auth_type = 'API_KEY_HEADER' AND auth_header_name IS NOT NULL AND credential_ciphertext IS NOT NULL)
    )
);
