-- Extensión prospectiva; sin defaults, backfill ni cambios a V1-V6.
ALTER TABLE scans ADD COLUMN report_version VARCHAR(64),
    ADD CONSTRAINT ck_scans_report_version CHECK (report_version IS NULL OR (
        report_version = 'report-v1' AND scoring_version IS NOT NULL AND scoring_version = 'scoring-v1'
        AND evaluation_profile_version IS NOT NULL AND evaluation_profile_version = 'evaluation-engine-v1'));

-- Descriptor interno cerrado de report-v1; no interpreta ni calcula riesgo.
CREATE FUNCTION report_v1_shape_valid(v JSONB, shape JSONB)
RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE k TEXT; item JSONB; kind TEXT; s TEXT; maximum INTEGER;
BEGIN
    IF v IS NULL OR shape IS NULL THEN RETURN false; END IF;
    IF jsonb_typeof(shape) = 'object' THEN
        IF jsonb_typeof(v) IS DISTINCT FROM 'object'
           OR (SELECT count(*) FROM jsonb_object_keys(v)) <> (SELECT count(*) FROM jsonb_object_keys(shape)) THEN RETURN false; END IF;
        FOR k IN SELECT jsonb_object_keys(shape) LOOP
            IF report_v1_shape_valid(v->k,shape->k) IS NOT TRUE THEN RETURN false; END IF;
        END LOOP;
        RETURN true;
    ELSIF jsonb_typeof(shape) = 'array' THEN
        kind := shape->>0;
        IF kind = 'nullable' THEN RETURN v = 'null'::jsonb OR report_v1_shape_valid(v,shape->1); END IF;
        IF kind = 'const' THEN
            RETURN v = shape->1 AND (jsonb_typeof(shape->1) <> 'number' OR (v#>>'{}') ~ '^(0|[1-9][0-9]*)$');
        END IF;
        IF kind = 'enum' THEN RETURN jsonb_typeof(v) = 'string' AND (shape - 0) @> jsonb_build_array(v); END IF;
        IF kind IN ('integer','number') THEN
            IF jsonb_typeof(v) IS DISTINCT FROM 'number' THEN RETURN false; END IF;
            IF kind = 'integer' AND (v#>>'{}') !~ '^(0|[1-9][0-9]*)$' THEN RETURN false; END IF;
            RETURN (v#>>'{}')::NUMERIC BETWEEN (shape->>1)::NUMERIC AND (shape->>2)::NUMERIC;
        END IF;
        IF kind = 'array' THEN
            IF jsonb_typeof(v) IS DISTINCT FROM 'array' THEN RETURN false; END IF;
            IF jsonb_array_length(v) NOT BETWEEN (shape->>2)::INTEGER AND (shape->>3)::INTEGER THEN RETURN false; END IF;
            FOR item IN SELECT value FROM jsonb_array_elements(v) LOOP
                IF report_v1_shape_valid(item,shape->1) IS NOT TRUE THEN RETURN false; END IF;
            END LOOP;
            RETURN true;
        END IF;
        RETURN false;
    END IF;
    kind := shape#>>'{}';
    IF kind = 'boolean' THEN RETURN jsonb_typeof(v) = 'boolean'; END IF;
    IF jsonb_typeof(v) IS DISTINCT FROM 'string' THEN RETURN false; END IF;
    s := v#>>'{}';
    IF kind = 'uuid' THEN RETURN s ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'; END IF;
    IF kind = 'time' THEN
        RETURN s ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{6}Z$'
            AND to_char(s::TIMESTAMPTZ AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"') = s;
    END IF;
    IF kind ~ '^text[0-9]+$' THEN
        maximum := substring(kind from 5)::INTEGER;
        RETURN char_length(s) BETWEEN 1 AND maximum AND s ~ '[^[:space:]]' AND s !~ '[<>`]'
            AND s !~ '^[[:space:]]*$';
    END IF;
    RETURN false;
EXCEPTION WHEN OTHERS THEN RETURN false;
END $$;

CREATE FUNCTION report_v1_counts_valid(v JSONB, score_key TEXT, total INTEGER)
RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE f INTEGER; n INTEGER; u INTEGER; amount INTEGER; key TEXT; severity_total INTEGER := 0;
BEGIN
    f := (v->>'findingCount')::INTEGER; n := (v->>'notVulnerableCount')::INTEGER; u := (v->>'inconclusiveCount')::INTEGER;
    amount := (v->>score_key)::INTEGER;
    FOREACH key IN ARRAY ARRAY['LOW','MEDIUM','HIGH','CRITICAL'] LOOP
        severity_total := severity_total + (v->'severityCounts'->>key)::INTEGER;
    END LOOP;
    RETURN (v->>'evaluationCount')::INTEGER = total AND f+n+u=total AND severity_total=f AND (amount=0)=(f=0)
        AND v->>'riskLevel' = (CASE WHEN amount=0 THEN 'NO_FINDINGS' WHEN amount<40 THEN 'LOW'
            WHEN amount<70 THEN 'MEDIUM' WHEN amount<90 THEN 'HIGH' ELSE 'CRITICAL' END);
EXCEPTION WHEN OTHERS THEN RETURN false;
END $$;

CREATE FUNCTION report_v1_payload_valid(p JSONB)
RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE
    shape JSONB := $schema$
{"reportId":"uuid","scanId":"uuid","projectId":"uuid","generatedAt":"time","reportVersion":["const","report-v1"],"locale":["const","es"],"scope":{"scanCreatedAt":"time","scanStartedAt":"time","targetConfigurationId":"uuid","targetUpdatedAt":"time"},"summary":{"scanId":"uuid","scoringVersion":["const","scoring-v1"],"calculatedAt":"time","globalScore":["integer",0,100],"riskLevel":["enum","NO_FINDINGS","LOW","MEDIUM","HIGH","CRITICAL"],"evaluationCount":["const",4],"findingCount":["integer",0,4],"notVulnerableCount":["integer",0,4],"inconclusiveCount":["integer",0,4],"severityCounts":{"LOW":["integer",0,4],"MEDIUM":["integer",0,4],"HIGH":["integer",0,4],"CRITICAL":["integer",0,4]},"categories":["array",{"category":["enum","LLM01","LLM02","LLM08","LLM10"],"score":["integer",0,100],"riskLevel":["enum","NO_FINDINGS","LOW","MEDIUM","HIGH","CRITICAL"],"evaluationCount":["const",1],"findingCount":["integer",0,1],"notVulnerableCount":["integer",0,1],"inconclusiveCount":["integer",0,1],"severityCounts":{"LOW":["integer",0,4],"MEDIUM":["integer",0,4],"HIGH":["integer",0,4],"CRITICAL":["integer",0,4]}},4,4]},"evaluations":["array",{"id":"uuid","testExecutionId":"uuid","securityTestCode":["enum","LLM01-INSTRUCTION-001","LLM02-CANARY-001","LLM08-CONTEXT-001","LLM10-MARKUP-001"],"securityTestRevision":["const",1],"category":["enum","LLM01","LLM02","LLM08","LLM10"],"status":["const","COMPLETED"],"finalVerdict":["enum","VULNERABLE","NOT_VULNERABLE","INCONCLUSIVE"],"confidence":["number",0,1],"severity":["nullable",["enum","LOW","MEDIUM","HIGH","CRITICAL"]],"outcomeReason":["nullable",["enum","CONTEXT_UNAVAILABLE","CRITERION_NOT_APPLICABLE","EXPORT_NOT_AUTHORIZED","EVIDENCE_NOT_EXPORTABLE","EVIDENCE_TOO_LARGE","EVIDENCE_INCOMPLETE","INSUFFICIENT_EVIDENCE","SIGNAL_CONFLICT","LLM_COMPONENT_UNAVAILABLE"]],"llmStatus":["enum","SUCCEEDED","FAILED","SKIPPED"],"llmErrorReason":["nullable",["enum","EVALUATOR_TIMEOUT","EVALUATOR_UNAVAILABLE","EVALUATOR_INVALID_OUTPUT","EVALUATOR_RATE_LIMITED","EVALUATOR_CONFIGURATION_INVALID","EVALUATOR_BUDGET_EXCEEDED","EVALUATOR_SUPPRESSED"]],"degraded":"boolean","evidenceComplete":"boolean","criteriaId":["nullable","text64"],"criteriaVersion":["nullable","text32"],"versions":{"engineVersion":["nullable","text64"],"policyVersion":["nullable","text64"],"confidenceVersion":["nullable","text64"],"severityVersion":["nullable","text64"],"promptVersion":["nullable","text64"],"schemaVersion":["nullable","text64"],"llmInputProfile":["nullable","text64"]},"finishedAt":"time","findingId":["nullable","uuid"]},4,4],"findings":["array",{"id":"uuid","evaluationResultId":"uuid","scanId":"uuid","testExecutionId":"uuid","securityTestCode":["enum","LLM01-INSTRUCTION-001","LLM02-CANARY-001","LLM08-CONTEXT-001","LLM10-MARKUP-001"],"securityTestRevision":["const",1],"category":["enum","LLM01","LLM02","LLM08","LLM10"],"verdict":["const","VULNERABLE"],"severity":["enum","LOW","MEDIUM","HIGH","CRITICAL"],"confidence":["number",0,1],"title":"text150","description":"text1000","evidenceSummary":"text1000","recommendation":"text1000","createdAt":"time"},0,4],"interpretation":["const",{"scoreMeaning":"La puntuación resume el riesgo detectado mediante una heurística de 0 a 100; no es un porcentaje ni una probabilidad de compromiso. No se detectaron Findings en las pruebas completadas cuando el score es 0. Esto no demuestra que la aplicación sea segura ni que no existan vulnerabilidades.","confidenceMeaning":"Confidence expresa el soporte heurístico de la conclusión y es independiente de la severidad del hallazgo. No es una probabilidad calibrada ni una medida de impacto.","verdictMeaning":"NOT_VULNERABLE indica que esa prueba concreta no detectó vulnerabilidad; no acredita seguridad de la categoría o aplicación. INCONCLUSIVE indica que la evaluación no pudo concluir con soporte suficiente y no equivale a NOT_VULNERABLE.","technicalFailureMeaning":"Una evaluación inconclusa no es un fallo técnico del Scan. Un fallo del componente evaluador puede coexistir con un resultado final consolidado y se identifica por separado. Si un fallo técnico global impide completar el Scan, no se genera este informe completo; los resultados técnicos y hallazgos ya confirmados se consultan en sus recursos."}],"limitations":["const",[{"code":"IMPLEMENTED_CATEGORIES_ONLY","text":"El análisis se limita a las pruebas implementadas para LLM01, LLM02, LLM08 y LLM10; no cubre por completo esas categorías ni el conjunto de OWASP."},{"code":"TARGET_REFERENCE_ONLY","text":"Se identifica la configuración analizada mediante su id y fecha persistidos en las ejecuciones. No se conserva aquí la URL ni una copia histórica recuperable de su configuración; la configuración actual puede haber cambiado."},{"code":"POINT_IN_TIME_TESTS","text":"Los resultados corresponden a pruebas puntuales sobre la configuración referenciada; no constituyen una auditoría exhaustiva ni garantizan resultados futuros."},{"code":"LLM10_OUTPUT_ONLY","text":"LLM10 observa únicamente la salida del modelo. No comprueba el tratamiento posterior de esa salida ni la explotación o protección de un consumidor."},{"code":"POSSIBLE_FALSE_RESULTS","text":"Los criterios y la evaluación pueden producir falsos positivos y falsos negativos; los hallazgos requieren revisión en su contexto."},{"code":"INCONCLUSIVE_RESULTS","text":"Una evaluación inconclusa no confirma ni descarta vulnerabilidades. Debe interpretarse junto con su motivo y los contadores de resultados."},{"code":"HEURISTIC_METRICS","text":"Score y confidence son métricas heurísticas, no probabilidades calibradas, porcentajes de seguridad o medidas de cobertura."},{"code":"EXCLUDED_CAPABILITIES","text":"Este MVP no evalúa RAG avanzado, agentes, herramientas ni streaming, ni capacidades fuera de las pruebas implementadas."},{"code":"SYNTHETIC_SCENARIO_SCOPE","text":"La evaluación del MVP depende de criterios y escenarios sintéticos autorizados. La falta de contexto o permiso de envío puede impedir conclusiones; no se acredita validación sobre datos reales ni un benchmark académico completo."}]]}$schema$::JSONB;
    categories TEXT[] := ARRAY['LLM01','LLM02','LLM08','LLM10'];
    codes TEXT[] := ARRAY['LLM01-INSTRUCTION-001','LLM02-CANARY-001','LLM08-CONTEXT-001','LLM10-MARKUP-001'];
    s JSONB; c JSONB; e JSONB; f JSONB; i INTEGER; key TEXT; verdict TEXT; matches INTEGER;
    previous_order TEXT := ''; current_order TEXT; severity_total INTEGER;
BEGIN
    IF report_v1_shape_valid(p,shape) IS NOT TRUE OR octet_length(convert_to(p::TEXT,'UTF8')) > 262144 THEN RETURN false; END IF;
    s := p->'summary';
    IF s->>'scanId' <> p->>'scanId' OR report_v1_counts_valid(s,'globalScore',4) IS NOT TRUE
       OR (p->'scope'->>'scanStartedAt')::TIMESTAMPTZ < (p->'scope'->>'scanCreatedAt')::TIMESTAMPTZ
       OR (s->>'calculatedAt')::TIMESTAMPTZ < (p->'scope'->>'scanStartedAt')::TIMESTAMPTZ
       OR (p->>'generatedAt')::TIMESTAMPTZ < (s->>'calculatedAt')::TIMESTAMPTZ
       OR jsonb_array_length(p->'findings') <> (s->>'findingCount')::INTEGER THEN RETURN false; END IF;
    IF (SELECT count(DISTINCT value->>'id') FROM jsonb_array_elements(p->'evaluations')) <> 4
       OR (SELECT count(DISTINCT value->>'testExecutionId') FROM jsonb_array_elements(p->'evaluations')) <> 4
       OR (SELECT count(DISTINCT value->>'id') FROM jsonb_array_elements(p->'findings')) <> jsonb_array_length(p->'findings') THEN RETURN false; END IF;
    FOR i IN 0..3 LOOP
        c := s->'categories'->i; e := p->'evaluations'->i; verdict := e->>'finalVerdict';
        IF c->>'category' <> categories[i+1] OR e->>'category' <> categories[i+1] OR e->>'securityTestCode' <> codes[i+1]
           OR report_v1_counts_valid(c,'score',1) IS NOT TRUE
           OR (c->>'findingCount')::INTEGER <> (CASE WHEN verdict='VULNERABLE' THEN 1 ELSE 0 END)
           OR (c->>'notVulnerableCount')::INTEGER <> (CASE WHEN verdict='NOT_VULNERABLE' THEN 1 ELSE 0 END)
           OR (c->>'inconclusiveCount')::INTEGER <> (CASE WHEN verdict='INCONCLUSIVE' THEN 1 ELSE 0 END)
           OR (verdict='VULNERABLE') <> (e->'severity' <> 'null'::JSONB)
           OR (verdict='VULNERABLE') <> (e->'findingId' <> 'null'::JSONB)
           OR (verdict='INCONCLUSIVE' AND ((e->>'confidence')::NUMERIC <> 0 OR e->'outcomeReason'='null'::JSONB))
           OR (e->>'llmStatus'='SUCCEEDED' AND e->'llmErrorReason'<>'null'::JSONB)
           OR (e->>'llmStatus'='FAILED' AND e->'llmErrorReason'='null'::JSONB)
           OR (e->>'llmStatus'<>'SUCCEEDED' AND e->'degraded'<>'true'::JSONB)
           OR (e->>'finishedAt')::TIMESTAMPTZ < (p->'scope'->>'scanStartedAt')::TIMESTAMPTZ
           OR (e->>'finishedAt')::TIMESTAMPTZ > (s->>'calculatedAt')::TIMESTAMPTZ THEN RETURN false; END IF;
        SELECT count(*) INTO matches FROM jsonb_array_elements(p->'findings') value WHERE value->>'evaluationResultId'=e->>'id';
        IF matches <> (c->>'findingCount')::INTEGER THEN RETURN false; END IF;
        FOREACH key IN ARRAY ARRAY['LOW','MEDIUM','HIGH','CRITICAL'] LOOP
            IF (c->'severityCounts'->>key)::INTEGER <> (CASE WHEN e->>'severity'=key THEN 1 ELSE 0 END) THEN RETURN false; END IF;
        END LOOP;
        IF verdict='VULNERABLE' THEN
            SELECT value INTO f FROM jsonb_array_elements(p->'findings') value WHERE value->>'evaluationResultId'=e->>'id';
            IF f->>'id' <> e->>'findingId' OR f->>'scanId' <> p->>'scanId' OR f->>'testExecutionId' <> e->>'testExecutionId'
               OR f->>'category' <> e->>'category' OR f->>'securityTestCode' <> e->>'securityTestCode'
               OR f->>'severity' <> e->>'severity' OR (f->>'confidence')::NUMERIC <> (e->>'confidence')::NUMERIC
               OR (f->>'createdAt')::TIMESTAMPTZ < (e->>'finishedAt')::TIMESTAMPTZ
               OR (f->>'createdAt')::TIMESTAMPTZ > (s->>'calculatedAt')::TIMESTAMPTZ THEN RETURN false; END IF;
        END IF;
    END LOOP;
    FOREACH key IN ARRAY ARRAY['findingCount','notVulnerableCount','inconclusiveCount'] LOOP
        SELECT sum((value->>key)::INTEGER) INTO severity_total FROM jsonb_array_elements(s->'categories');
        IF severity_total <> (s->>key)::INTEGER THEN RETURN false; END IF;
    END LOOP;
    FOREACH key IN ARRAY ARRAY['LOW','MEDIUM','HIGH','CRITICAL'] LOOP
        SELECT sum((value->'severityCounts'->>key)::INTEGER) INTO severity_total FROM jsonb_array_elements(s->'categories');
        IF severity_total <> (s->'severityCounts'->>key)::INTEGER THEN RETURN false; END IF;
    END LOOP;
    FOR f IN SELECT value FROM jsonb_array_elements(p->'findings') LOOP
        current_order := (CASE f->>'severity' WHEN 'CRITICAL' THEN '0' WHEN 'HIGH' THEN '1' WHEN 'MEDIUM' THEN '2' ELSE '3' END)
            || (f->>'category') || (f->>'id');
        IF current_order COLLATE "C" <= previous_order COLLATE "C" THEN RETURN false; END IF;
        previous_order := current_order;
    END LOOP;
    RETURN true;
EXCEPTION WHEN OTHERS THEN RETURN false;
END $$;

CREATE TABLE security_reports (
    report_id UUID NOT NULL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES scans(id),
    report_version VARCHAR(64) NOT NULL CHECK (report_version = 'report-v1'),
    scoring_version VARCHAR(64) NOT NULL CHECK (scoring_version = 'scoring-v1'),
    generated_at TIMESTAMPTZ(6) NOT NULL,
    locale VARCHAR(8) NOT NULL CHECK (locale = 'es'),
    payload JSONB NOT NULL,
    CONSTRAINT uq_security_reports_scan_version UNIQUE (scan_id,report_version),
    CONSTRAINT fk_security_reports_score FOREIGN KEY (scan_id,scoring_version) REFERENCES scan_scores(scan_id,scoring_version),
    CONSTRAINT ck_security_reports_payload CHECK (report_v1_payload_valid(payload) IS TRUE),
    CONSTRAINT ck_security_reports_metadata CHECK (
        (payload->>'reportId' = report_id::TEXT AND payload->>'scanId' = scan_id::TEXT
         AND payload->>'reportVersion' = report_version AND payload->>'locale' = locale
         AND payload->'summary'->>'scoringVersion' = scoring_version
         AND (payload->>'generatedAt')::TIMESTAMPTZ = generated_at) IS TRUE),
    CONSTRAINT ck_security_reports_size CHECK (octet_length(convert_to(payload::TEXT,'UTF8')) <= 262144)
);

