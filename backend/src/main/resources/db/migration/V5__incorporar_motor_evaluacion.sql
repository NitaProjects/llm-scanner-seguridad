-- Conservadora y transaccional: no inferir ni eliminar evaluaciones históricas.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM findings)
       OR EXISTS (SELECT 1 FROM test_executions WHERE evaluator_result IS NOT NULL OR confidence IS NOT NULL) THEN
        RAISE EXCEPTION 'V5 requiere una conversión explícita de los históricos semánticos existentes';
    END IF;
END $$;

ALTER TABLE scans ADD COLUMN evaluation_profile_version VARCHAR(64),
    ADD CONSTRAINT ck_scans_evaluation_profile CHECK (
        evaluation_profile_version IS NULL OR evaluation_profile_version = 'evaluation-engine-v1');
ALTER TABLE test_executions ADD CONSTRAINT uq_test_executions_evaluation_identity UNIQUE (id, scan_id, test_case_id);

CREATE TABLE evaluation_results (
    id UUID PRIMARY KEY,
    test_execution_id UUID NOT NULL UNIQUE,
    scan_id UUID NOT NULL REFERENCES scans(id),
    test_case_id UUID NOT NULL REFERENCES test_cases(id),
    category VARCHAR(10) NOT NULL CHECK (category IN ('LLM01','LLM02','LLM08','LLM10')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED')),
    created_at TIMESTAMPTZ(6) NOT NULL,
    started_at TIMESTAMPTZ(6),
    finished_at TIMESTAMPTZ(6),
    error_reason VARCHAR(40) CHECK (error_reason IS NULL OR error_reason IN (
        'CRITERIA_UNAVAILABLE','EVALUATION_INTERNAL_ERROR','SCAN_BUDGET_EXCEEDED','EVALUATION_ABORTED','EVALUATION_INTERRUPTED')),
    outcome_reason VARCHAR(40) CHECK (outcome_reason IS NULL OR outcome_reason IN (
        'CONTEXT_UNAVAILABLE','CRITERION_NOT_APPLICABLE','EXPORT_NOT_AUTHORIZED','EVIDENCE_NOT_EXPORTABLE',
        'EVIDENCE_TOO_LARGE','EVIDENCE_INCOMPLETE','INSUFFICIENT_EVIDENCE','SIGNAL_CONFLICT','LLM_COMPONENT_UNAVAILABLE')),
    final_verdict VARCHAR(20) CHECK (final_verdict IS NULL OR final_verdict IN ('VULNERABLE','NOT_VULNERABLE','INCONCLUSIVE')),
    deterministic_verdict VARCHAR(20) CHECK (deterministic_verdict IS NULL OR deterministic_verdict IN ('VULNERABLE','NOT_VULNERABLE','INCONCLUSIVE')),
    severity VARCHAR(10) CHECK (severity IS NULL OR severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    confidence NUMERIC(3,2) CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1),
    confidence_components JSONB,
    decision_code VARCHAR(20) CHECK (decision_code IS NULL OR decision_code IN ('HYB-01','HYB-02','HYB-03','HYB-04','HYB-05','HYB-06','HYB-07')),
    strategy VARCHAR(20) NOT NULL DEFAULT 'HYBRID' CHECK (strategy = 'HYBRID'),
    degraded BOOLEAN NOT NULL DEFAULT false,
    verdict_discrepancy BOOLEAN NOT NULL DEFAULT false,
    severity_discrepancy BOOLEAN NOT NULL DEFAULT false,
    deterministic_signals JSONB NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(deterministic_signals) = 'array'),
    llm_assessment JSONB,
    llm_status VARCHAR(20) NOT NULL CHECK (llm_status IN ('NOT_REQUESTED','SUCCEEDED','FAILED','SKIPPED')),
    llm_error_reason VARCHAR(40) CHECK (llm_error_reason IS NULL OR llm_error_reason IN (
        'EVALUATOR_TIMEOUT','EVALUATOR_UNAVAILABLE','EVALUATOR_INVALID_OUTPUT','EVALUATOR_RATE_LIMITED',
        'EVALUATOR_CONFIGURATION_INVALID','EVALUATOR_BUDGET_EXCEEDED','EVALUATOR_SUPPRESSED')),
    llm_attempt_count SMALLINT NOT NULL DEFAULT 0 CHECK (llm_attempt_count BETWEEN 0 AND 1),
    reasoning_summary TEXT,
    recommendation TEXT,
    evidence_summary TEXT,
    criteria_id VARCHAR(64), criteria_version VARCHAR(32), scenario_id VARCHAR(64), scenario_version VARCHAR(32),
    engine_version VARCHAR(64), policy_version VARCHAR(64), confidence_version VARCHAR(64), severity_version VARCHAR(64),
    prompt_version VARCHAR(64), schema_version VARCHAR(64), llm_input_profile VARCHAR(64),
    configuration_snapshot JSONB, configuration_hash VARCHAR(64),
    evidence_projection TEXT, evidence_complete BOOLEAN NOT NULL DEFAULT false, redacted BOOLEAN NOT NULL DEFAULT false,
    input_bytes INTEGER CHECK (input_bytes IS NULL OR input_bytes >= 0),
    evidence_bytes INTEGER CHECK (evidence_bytes IS NULL OR evidence_bytes >= 0), input_hash VARCHAR(64),
    requested_provider VARCHAR(64), requested_model VARCHAR(128), resolved_model VARCHAR(128), sampling_parameters JSONB,
    latency_ms BIGINT CHECK (latency_ms IS NULL OR latency_ms >= 0),
    input_tokens BIGINT CHECK (input_tokens IS NULL OR input_tokens >= 0),
    output_tokens BIGINT CHECK (output_tokens IS NULL OR output_tokens >= 0),
    reserved_cost_usd NUMERIC(12,6) CHECK (reserved_cost_usd IS NULL OR reserved_cost_usd >= 0),
    estimated_cost_usd NUMERIC(12,6) CHECK (estimated_cost_usd IS NULL OR estimated_cost_usd >= 0),
    pricing_version VARCHAR(64), estimate_version VARCHAR(64), provider_request_id VARCHAR(128),
    CONSTRAINT fk_evaluation_execution FOREIGN KEY (test_execution_id,scan_id,test_case_id)
        REFERENCES test_executions(id,scan_id,test_case_id),
    CONSTRAINT uq_evaluation_finding_identity UNIQUE
        (id,test_execution_id,scan_id,test_case_id,category,final_verdict,severity,confidence),
    CONSTRAINT ck_evaluation_dates CHECK (
        (started_at IS NULL OR started_at >= created_at)
        AND (finished_at IS NULL OR finished_at >= created_at)
        AND (started_at IS NULL OR finished_at IS NULL OR finished_at >= started_at)),
    CONSTRAINT ck_evaluation_severity CHECK (
        (final_verdict IS NULL AND severity IS NULL)
        OR (final_verdict IS NOT NULL AND final_verdict = 'VULNERABLE' AND severity IS NOT NULL)
        OR (final_verdict IS NOT NULL AND final_verdict IN ('NOT_VULNERABLE','INCONCLUSIVE') AND severity IS NULL)),
    CONSTRAINT ck_evaluation_llm CHECK (
        (llm_status = 'SUCCEEDED' AND llm_attempt_count = 1 AND llm_assessment IS NOT NULL
            AND jsonb_typeof(llm_assessment) = 'object' AND llm_error_reason IS NULL)
        OR (llm_status = 'FAILED' AND llm_attempt_count = 1 AND llm_assessment IS NULL AND llm_error_reason IS NOT NULL)
        OR (llm_status = 'SKIPPED' AND llm_attempt_count = 0 AND llm_assessment IS NULL)
        OR (llm_status = 'NOT_REQUESTED' AND llm_assessment IS NULL AND llm_error_reason IS NULL)),
    CONSTRAINT ck_evaluation_lifecycle CHECK (
        (status = 'PENDING' AND started_at IS NULL AND finished_at IS NULL AND error_reason IS NULL
            AND final_verdict IS NULL AND confidence IS NULL AND decision_code IS NULL
            AND llm_status = 'NOT_REQUESTED' AND llm_attempt_count = 0)
        OR (status = 'RUNNING' AND started_at IS NOT NULL AND finished_at IS NULL AND error_reason IS NULL
            AND final_verdict IS NULL AND confidence IS NULL AND decision_code IS NULL)
        OR (status = 'COMPLETED' AND started_at IS NOT NULL AND finished_at IS NOT NULL AND error_reason IS NULL
            AND final_verdict IS NOT NULL AND confidence IS NOT NULL AND decision_code IS NOT NULL
            AND confidence_components IS NOT NULL AND jsonb_typeof(confidence_components) = 'object'
            AND reasoning_summary IS NOT NULL AND recommendation IS NOT NULL
            AND (final_verdict <> 'INCONCLUSIVE' OR (confidence = 0 AND outcome_reason IS NOT NULL))
            AND ((llm_status = 'SUCCEEDED' AND llm_error_reason IS NULL)
                OR (llm_status IN ('FAILED','SKIPPED') AND degraded)))
        OR (status = 'FAILED' AND finished_at IS NOT NULL AND error_reason IS NOT NULL
            AND final_verdict IS NOT NULL AND final_verdict = 'INCONCLUSIVE'
            AND confidence IS NOT NULL AND confidence = 0 AND severity IS NULL)),
    CONSTRAINT ck_evaluation_sizes CHECK (
        (configuration_snapshot IS NULL OR jsonb_typeof(configuration_snapshot) = 'object')
        AND (sampling_parameters IS NULL OR jsonb_typeof(sampling_parameters) = 'object')
        AND char_length(coalesce(reasoning_summary,'')) <= 1000
        AND char_length(coalesce(recommendation,'')) <= 1000
        AND char_length(coalesce(evidence_summary,'')) <= 1000
        AND octet_length(coalesce(configuration_snapshot::text,''))
            + octet_length(coalesce(evidence_projection,'')) + octet_length(deterministic_signals::text)
            + octet_length(coalesce(llm_assessment::text,'')) + octet_length(coalesce(confidence_components::text,''))
            + octet_length(coalesce(reasoning_summary,'')) + octet_length(coalesce(recommendation,''))
            + octet_length(coalesce(evidence_summary,'')) <= 65536)
);
CREATE INDEX idx_evaluation_results_scan_order ON evaluation_results(scan_id,test_case_id,id);

ALTER TABLE findings
    ALTER COLUMN test_case_id SET NOT NULL,
    ALTER COLUMN test_execution_id SET NOT NULL,
    ALTER COLUMN confidence SET NOT NULL,
    ALTER COLUMN evidence SET NOT NULL,
    ALTER COLUMN explanation SET NOT NULL,
    ALTER COLUMN recommendation SET NOT NULL,
    ALTER COLUMN created_at TYPE TIMESTAMPTZ(6),
    ADD COLUMN evaluation_result_id UUID NOT NULL REFERENCES evaluation_results(id),
    ADD COLUMN title VARCHAR(150) NOT NULL,
    ADD COLUMN final_verdict VARCHAR(20) NOT NULL CHECK (final_verdict = 'VULNERABLE'),
    ADD CONSTRAINT uq_findings_execution UNIQUE (test_execution_id),
    ADD CONSTRAINT uq_findings_evaluation UNIQUE (evaluation_result_id),
    ADD CONSTRAINT ck_findings_confidence CHECK (confidence BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_findings_severity CHECK (risk_level IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    ADD CONSTRAINT ck_findings_text CHECK (char_length(explanation) <= 1000 AND char_length(evidence) <= 1000
        AND char_length(recommendation) <= 1000 AND title ~ '[^[:space:]]'),
    ADD CONSTRAINT fk_findings_evaluation_consistent FOREIGN KEY
        (evaluation_result_id,test_execution_id,scan_id,test_case_id,owasp_category,final_verdict,risk_level,confidence)
        REFERENCES evaluation_results(id,test_execution_id,scan_id,test_case_id,category,final_verdict,severity,confidence);
