-- Extensión prospectiva: no modifica V1-V5 ni calcula resultados históricos.
ALTER TABLE scans ADD COLUMN scoring_version VARCHAR(64),
    ADD CONSTRAINT ck_scans_scoring_version CHECK (
        scoring_version IS NULL OR (scoring_version = 'scoring-v1'
            AND evaluation_profile_version IS NOT NULL AND evaluation_profile_version = 'evaluation-engine-v1'));

-- Validación cerrada de los VOs JSONB contenidos en una única fila de ScanScore.
CREATE FUNCTION scoring_v1_summary_valid(value INTEGER, level TEXT, evaluations INTEGER,
        findings INTEGER, negatives INTEGER, inconclusives INTEGER, severities JSONB)
RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE
    key TEXT;
    severity_total INTEGER := 0;
    base INTEGER;
BEGIN
    IF value IS NULL OR level IS NULL OR evaluations IS NULL OR findings IS NULL OR negatives IS NULL
       OR inconclusives IS NULL OR severities IS NULL OR jsonb_typeof(severities) <> 'object'
       OR NOT severities ?& ARRAY['LOW','MEDIUM','HIGH','CRITICAL']
       OR severities - ARRAY['LOW','MEDIUM','HIGH','CRITICAL'] <> '{}'::jsonb
       OR value NOT BETWEEN 0 AND 100 OR evaluations NOT BETWEEN 0 AND 4
       OR findings NOT BETWEEN 0 AND 4 OR negatives NOT BETWEEN 0 AND 4 OR inconclusives NOT BETWEEN 0 AND 4
       OR evaluations <> findings + negatives + inconclusives THEN RETURN false; END IF;
    FOREACH key IN ARRAY ARRAY['LOW','MEDIUM','HIGH','CRITICAL'] LOOP
        IF jsonb_typeof(severities -> key) <> 'number' OR (severities ->> key) !~ '^[0-4]$' THEN RETURN false; END IF;
        severity_total := severity_total + (severities ->> key)::INTEGER;
    END LOOP;
    IF severity_total <> findings OR (value = 0) <> (findings = 0)
       OR level <> (CASE WHEN value = 0 THEN 'NO_FINDINGS' WHEN value < 40 THEN 'LOW'
           WHEN value < 70 THEN 'MEDIUM' WHEN value < 90 THEN 'HIGH' ELSE 'CRITICAL' END) THEN RETURN false; END IF;
    IF findings > 0 THEN
        base := CASE WHEN (severities ->> 'CRITICAL')::INTEGER > 0 THEN 90
            WHEN (severities ->> 'HIGH')::INTEGER > 0 THEN 70
            WHEN (severities ->> 'MEDIUM')::INTEGER > 0 THEN 40 ELSE 10 END;
        IF value < base OR value > least(100, base + CASE WHEN evaluations = 1 THEN 5 ELSE 15 END) THEN RETURN false; END IF;
    END IF;
    RETURN true;
END $$;

CREATE FUNCTION scoring_v1_categories_valid(entries JSONB, value INTEGER, findings INTEGER,
        negatives INTEGER, inconclusives INTEGER, severities JSONB)
RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE
    entry JSONB;
    index INTEGER := 0;
    key TEXT;
    names TEXT[] := ARRAY['LLM01','LLM02','LLM08','LLM10'];
    fields TEXT[] := ARRAY['category','score','riskLevel','evaluationCount','findingCount','notVulnerableCount','inconclusiveCount','severityCounts'];
    total_findings INTEGER := 0;
    total_negatives INTEGER := 0;
    total_inconclusives INTEGER := 0;
    maximum INTEGER := 0;
    weights INTEGER := 0;
    count_value INTEGER;
BEGIN
    IF entries IS NULL OR jsonb_typeof(entries) <> 'array' OR jsonb_array_length(entries) <> 4 THEN RETURN false; END IF;
    FOR entry IN SELECT * FROM jsonb_array_elements(entries) LOOP
        index := index + 1;
        IF jsonb_typeof(entry) <> 'object' OR NOT entry ?& fields OR entry - fields <> '{}'::jsonb
           OR jsonb_typeof(entry -> 'category') <> 'string' OR entry ->> 'category' <> names[index]
           OR jsonb_typeof(entry -> 'riskLevel') <> 'string' THEN RETURN false; END IF;
        FOREACH key IN ARRAY ARRAY['score','evaluationCount','findingCount','notVulnerableCount','inconclusiveCount'] LOOP
            IF jsonb_typeof(entry -> key) <> 'number' OR (entry ->> key) !~ '^(0|[1-9][0-9]{0,2})$' THEN RETURN false; END IF;
        END LOOP;
        IF (entry ->> 'evaluationCount')::INTEGER <> 1 OR NOT scoring_v1_summary_valid(
            (entry ->> 'score')::INTEGER, entry ->> 'riskLevel', (entry ->> 'evaluationCount')::INTEGER,
            (entry ->> 'findingCount')::INTEGER, (entry ->> 'notVulnerableCount')::INTEGER,
            (entry ->> 'inconclusiveCount')::INTEGER, entry -> 'severityCounts') THEN RETURN false; END IF;
        total_findings := total_findings + (entry ->> 'findingCount')::INTEGER;
        total_negatives := total_negatives + (entry ->> 'notVulnerableCount')::INTEGER;
        total_inconclusives := total_inconclusives + (entry ->> 'inconclusiveCount')::INTEGER;
        maximum := greatest(maximum, (entry ->> 'score')::INTEGER);
        weights := weights + (entry ->> 'score')::INTEGER;
    END LOOP;
    FOREACH key IN ARRAY ARRAY['LOW','MEDIUM','HIGH','CRITICAL'] LOOP
        SELECT sum((item -> 'severityCounts' ->> key)::INTEGER) INTO count_value FROM jsonb_array_elements(entries) item;
        IF count_value IS DISTINCT FROM (severities ->> key)::INTEGER THEN RETURN false; END IF;
    END LOOP;
    RETURN total_findings = findings AND total_negatives = negatives AND total_inconclusives = inconclusives
        AND value = least(100, maximum + least(10, (weights - maximum) / 10));
END $$;

CREATE TABLE scan_scores (
    scan_id UUID NOT NULL REFERENCES scans(id),
    scoring_version VARCHAR(64) NOT NULL CHECK (scoring_version = 'scoring-v1'),
    calculated_at TIMESTAMPTZ(6) NOT NULL,
    global_score SMALLINT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    evaluation_count SMALLINT NOT NULL CHECK (evaluation_count = 4),
    finding_count SMALLINT NOT NULL,
    not_vulnerable_count SMALLINT NOT NULL,
    inconclusive_count SMALLINT NOT NULL,
    severity_counts JSONB NOT NULL,
    categories JSONB NOT NULL,
    PRIMARY KEY (scan_id, scoring_version),
    CONSTRAINT ck_scan_scores_summary CHECK (scoring_v1_summary_valid(global_score::INTEGER, risk_level::TEXT,
        evaluation_count::INTEGER, finding_count::INTEGER, not_vulnerable_count::INTEGER,
        inconclusive_count::INTEGER, severity_counts) IS TRUE),
    CONSTRAINT ck_scan_scores_categories CHECK (scoring_v1_categories_valid(categories, global_score::INTEGER,
        finding_count::INTEGER, not_vulnerable_count::INTEGER, inconclusive_count::INTEGER, severity_counts) IS TRUE),
    CONSTRAINT ck_scan_scores_size CHECK (octet_length(severity_counts::TEXT) + octet_length(categories::TEXT) <= 16384)
);
