ALTER TABLE target_configurations
    ADD COLUMN adapter_type VARCHAR(32) NOT NULL DEFAULT 'REST_LEGACY',
    ADD COLUMN model VARCHAR(200),
    ADD COLUMN system_prompt TEXT,
    ALTER COLUMN request_template DROP NOT NULL,
    ALTER COLUMN response_path DROP NOT NULL,
    ADD CONSTRAINT ck_target_adapter_type CHECK (adapter_type IN ('REST_LEGACY', 'OPENAI_COMPATIBLE')),
    ADD CONSTRAINT ck_target_adapter_fields CHECK (
        (adapter_type = 'REST_LEGACY' AND request_template IS NOT NULL AND response_path IS NOT NULL
            AND model IS NULL AND system_prompt IS NULL)
        OR (adapter_type = 'OPENAI_COMPATIBLE' AND model IS NOT NULL AND btrim(model) <> ''
            AND request_template IS NULL AND response_path IS NULL AND auth_type = 'BEARER_TOKEN')
    ),
    ADD CONSTRAINT ck_target_system_prompt CHECK (
        system_prompt IS NULL OR (octet_length(system_prompt) <= 16384 AND btrim(system_prompt) <> '')
    );
