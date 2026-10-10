ALTER TABLE scans
    ALTER COLUMN created_at TYPE TIMESTAMPTZ(6),
    ALTER COLUMN started_at TYPE TIMESTAMPTZ(6),
    ALTER COLUMN finished_at TYPE TIMESTAMPTZ(6),
    ADD COLUMN error_reason VARCHAR(40),
    ADD CONSTRAINT ck_scans_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    ADD CONSTRAINT ck_scans_error_reason CHECK (
        error_reason IS NULL OR error_reason IN (
            'SCAN_DISPATCH_FAILED', 'SCAN_PROCESSING_FAILED', 'SCAN_INTERRUPTED'
        )
    ),
    ADD CONSTRAINT ck_scans_lifecycle CHECK (
        (started_at IS NULL OR started_at >= created_at)
        AND (finished_at IS NULL OR finished_at >= created_at)
        AND (started_at IS NULL OR finished_at IS NULL OR finished_at >= started_at)
        AND (
            (status = 'PENDING' AND started_at IS NULL AND finished_at IS NULL AND error_reason IS NULL)
            OR (status = 'RUNNING' AND started_at IS NOT NULL AND finished_at IS NULL AND error_reason IS NULL)
            OR (status = 'COMPLETED' AND started_at IS NOT NULL AND finished_at IS NOT NULL AND error_reason IS NULL)
            OR (status = 'FAILED' AND finished_at IS NOT NULL AND error_reason IS NOT NULL
                AND (error_reason <> 'SCAN_DISPATCH_FAILED' OR started_at IS NULL))
        )
    );

-- No corrige históricos incompatibles: PostgreSQL debe rechazar toda la migración.
CREATE UNIQUE INDEX uq_scans_active_project ON scans(project_id)
    WHERE status IN ('PENDING', 'RUNNING');
