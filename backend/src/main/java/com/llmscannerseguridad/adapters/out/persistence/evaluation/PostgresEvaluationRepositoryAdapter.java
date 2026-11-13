package com.llmscannerseguridad.adapters.out.persistence.evaluation;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import com.llmscannerseguridad.adapters.out.evaluation.*;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.finding.Finding;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresEvaluationRepositoryAdapter implements EvaluationRepositoryPort {
    private static final Set<String> JSON_COLUMNS = Set.of("confidence_components", "deterministic_signals", "llm_assessment", "configuration_snapshot", "sampling_parameters");
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final EvaluationCriteriaCatalogPort catalog;
    private final EvaluationScenarioPort scenarios;
    private final EvaluationEvidenceProjector projector;
    private final EvaluatorSettings settings;
    private final Clock clock;

    public PostgresEvaluationRepositoryAdapter(DataSource source, PlatformTransactionManager manager,
            EvaluationCriteriaCatalogPort catalog, EvaluationScenarioPort scenarios, EvaluationEvidenceProjector projector,
            EvaluatorSettings settings, Clock clock) {
        jdbc = new NamedParameterJdbcTemplate(source);
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.catalog = catalog; this.scenarios = scenarios; this.projector = projector; this.settings = settings; this.clock = clock;
    }
    private <T> T confirmed(Supplier<T> operation) {
        try { return transaction.execute(ignored -> operation.get()); }
        catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    private void participating() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw ScanOperationException.internal();
    }
    @Override public void setProfile(UUID scanId) {
        participating();
        if (jdbc.update("update scans set evaluation_profile_version = :profile where id = :id and status = 'PENDING'",
                Map.of("profile", EvaluationResult.PROFILE, "id", scanId)) != 1) throw ScanOperationException.internal();
    }
    @Override public String profile(UUID scanId) {
        try { return jdbc.queryForObject("select evaluation_profile_version from scans where id = :id", Map.of("id", scanId), String.class); }
        catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    @Override public void prepare(EvaluationPlan plan) {
        participating();
        var scenario = scenarios.snapshot(plan.projectId(), plan.targetConfigurationId(), plan.targetUpdatedAt(), plan.items());
        var preparedIds = new ArrayList<UUID>();
        TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) preparedIds.forEach(projector::release);
            }
        });
        for (var test : plan.items()) {
            var id = UUID.randomUUID();
            preparedIds.add(id);
            var config = projector.configuration(id, catalog.find(test.securityTestCode(), test.securityTestRevision(), test.category()), scenario);
            insert(EvaluationResult.pending(id, test.testExecutionId(), plan.scanId(), test.securityTestId(), test.category(),
                    EvaluationResult.latest(clock.instant(), plan.scanStartedAt()), config));
        }
    }
    @Override public Optional<EvaluationResult> findByExecution(UUID executionId) {
        try { return jdbc.query("select * from evaluation_results where test_execution_id = :id", Map.of("id", executionId), this::read).stream().findFirst(); }
        catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    @Override public Optional<EvaluationResult> claim(EvaluationResult prepared, boolean requestLlm) {
        return confirmed(() -> {
            var parent = jdbc.queryForObject("select status from scans where id = :id for update", Map.of("id", prepared.scanId()), String.class);
            if (!"RUNNING".equals(parent)) throw ScanOperationException.internal();
            var rows = jdbc.query("select * from evaluation_results where id = :id for update", Map.of("id", prepared.id()), this::read);
            if (rows.isEmpty() || rows.getFirst().status() != EvaluationStatus.PENDING) return Optional.empty();
            var current = rows.getFirst();
            if (!current.testExecutionId().equals(prepared.testExecutionId())) throw ScanOperationException.internal();
            int valid = jdbc.queryForObject("""
                    select count(*) from test_executions e join test_cases t on t.id=e.test_case_id
                    where e.id=:id and e.status='COMPLETED' and t.category=:category and e.finished_at <= :started
                    """, Map.of("id", prepared.testExecutionId(), "category", prepared.category().name(),
                        "started", prepared.startedAt().atOffset(ZoneOffset.UTC)), Integer.class);
            if (valid != 1) throw ScanOperationException.internal();
            var component = prepared.llm();
            if (requestLlm) {
                int suppressed = jdbc.queryForObject("""
                        select count(*) from evaluation_results where scan_id=:id and llm_error_reason in
                        ('EVALUATOR_RATE_LIMITED','EVALUATOR_CONFIGURATION_INVALID','EVALUATOR_BUDGET_EXCEEDED','EVALUATOR_SUPPRESSED')
                        """, Map.of("id", prepared.scanId()), Integer.class);
                int attempts = jdbc.queryForObject("select coalesce(sum(llm_attempt_count),0) from evaluation_results where scan_id=:id",
                        Map.of("id", prepared.scanId()), Integer.class);
                BigDecimal reserved = jdbc.queryForObject("select coalesce(sum(reserved_cost_usd),0) from evaluation_results where scan_id=:id",
                        Map.of("id", prepared.scanId()), BigDecimal.class);
                LlmError denied = suppressed > 0 ? LlmError.EVALUATOR_SUPPRESSED : attempts >= settings.maxCalls()
                        ? LlmError.EVALUATOR_BUDGET_EXCEEDED : null;
                BigDecimal reserve = null;
                if (denied == null && settings.maxEstimatedCostUsd() != null) {
                    if (!settings.pricingValid() || reserved.add(settings.estimatedMaxCallCostUsd()).compareTo(settings.maxEstimatedCostUsd()) > 0) {
                        denied = LlmError.EVALUATOR_BUDGET_EXCEEDED;
                    } else reserve = settings.estimatedMaxCallCostUsd();
                }
                component = new EvaluationResult.LlmComponent(denied == null ? LlmStatus.NOT_REQUESTED : LlmStatus.SKIPPED,
                        denied, denied == null ? 1 : 0, null, null, null, null, null, reserve, null,
                        settings.tariffValid() ? settings.pricingVersion() : null, settings.tariffValid() ? settings.estimateVersion() : null, null);
            }
            var running = replaceComponent(prepared, component);
            if (running.configuration() != null && "hybrid-v2".equals(running.configuration().policyVersion())) {
                running = running.withEvidence(running.evidence().withoutProjection());
            }
            if (update(running, "status = 'PENDING'") != 1) return Optional.empty();
            return Optional.of(running);
        });
    }
    private EvaluationResult replaceComponent(EvaluationResult result, EvaluationResult.LlmComponent component) {
        return new EvaluationResult(result.id(), result.testExecutionId(), result.scanId(), result.securityTestId(), result.category(),
                result.status(), result.createdAt(), result.startedAt(), result.finishedAt(), result.errorReason(), result.decision(),
                result.deterministicSignals(), component, result.configuration(), result.evidence(), result.reasoningSummary(), result.recommendation(), result.evidenceSummary());
    }
    @Override public void finish(EvaluationResult result) {
        confirmed(() -> {
            if (result.status().active()) throw ScanOperationException.internal();
            if (update(result, "status = 'RUNNING'") != 1) throw ScanOperationException.internal();
            if (result.status() == EvaluationStatus.COMPLETED && result.decision().finalVerdict() == Verdict.VULNERABLE) {
                var finding = Finding.from(result);
                jdbc.update("""
                        insert into findings(id,evaluation_result_id,test_execution_id,scan_id,test_case_id,owasp_category,
                            final_verdict,risk_level,confidence,title,explanation,evidence,recommendation,created_at)
                        values(:id,:evaluation,:execution,:scan,:test,:category,'VULNERABLE',:severity,:confidence,:title,:description,:evidence,:recommendation,:created)
                        """, findingValues(finding));
            }
            releaseAfterCommit(List.of(result.id()));
            return null;
        });
    }
    private Map<String,Object> findingValues(Finding f) {
        return Map.ofEntries(Map.entry("id", f.id()), Map.entry("evaluation", f.evaluationResultId()),
                Map.entry("execution", f.testExecutionId()), Map.entry("scan", f.scanId()), Map.entry("test", f.securityTestId()),
                Map.entry("category", f.category().name()), Map.entry("severity", f.severity().name()), Map.entry("confidence", f.confidence()),
                Map.entry("title", f.title()), Map.entry("description", f.description()), Map.entry("evidence", f.evidenceSummary()),
                Map.entry("recommendation", f.recommendation()), Map.entry("created", f.createdAt().atOffset(ZoneOffset.UTC)));
    }
    @Override public void abortExecution(UUID executionId, EvaluationError reason) {
        confirmed(() -> {
            var rows = jdbc.query("select * from evaluation_results where test_execution_id=:id for update", Map.of("id", executionId), this::read);
            for (var result : rows) if (result.status().active()) {
                update(result.fail(clock.instant(), reason), "status in ('PENDING','RUNNING')");
                releaseAfterCommit(List.of(result.id()));
            }
            return null;
        });
    }
    @Override public boolean allCompleted(UUID scanId) {
        if (profile(scanId) == null) return true;
        try { return jdbc.queryForObject("select count(*)=4 and count(*) filter(where status='COMPLETED')=4 from evaluation_results where scan_id=:id",
                Map.of("id", scanId), Boolean.class); }
        catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    @Override public void closeActive(UUID scanId, Instant now, EvaluationError reason) {
        participating();
        var rows = jdbc.query("select * from evaluation_results where scan_id=:id and status in ('PENDING','RUNNING') for update", Map.of("id", scanId), this::read);
        for (var result : rows) {
            update(result.fail(now, reason), "status in ('PENDING','RUNNING')");
            releaseAfterCommit(List.of(result.id()));
        }
    }
    private void releaseAfterCommit(List<UUID> ids) {
        TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCommit() { ids.forEach(projector::release); }
        });
    }
    @Override public void recoverActive(Instant now) {
        participating();
        // Solo registros de interrupción para filas ausentes de scans nuevos que estaban activos.
        var missing = jdbc.queryForList("""
                select e.id,e.scan_id,e.test_case_id,t.category from test_executions e
                join scans s on s.id=e.scan_id join test_cases t on t.id=e.test_case_id
                where s.evaluation_profile_version='evaluation-engine-v1' and s.status in ('PENDING','RUNNING')
                and e.status='COMPLETED' and not exists(select 1 from evaluation_results r where r.test_execution_id=e.id)
                """, Map.of());
        for (var row : missing) {
            var pending = EvaluationResult.pending(UUID.randomUUID(), (UUID)row.get("id"), (UUID)row.get("scan_id"),
                    (UUID)row.get("test_case_id"), SecurityCategory.valueOf((String)row.get("category")), now, null);
            insert(pending.fail(now, EvaluationError.EVALUATION_INTERRUPTED));
        }
        var ids = jdbc.queryForList("select distinct scan_id from evaluation_results where status in ('PENDING','RUNNING')", Map.of(), UUID.class);
        ids.forEach(id -> closeActive(id, now, EvaluationError.EVALUATION_INTERRUPTED));
    }
    private void insert(EvaluationResult result) {
        var values = values(result);
        String columns = String.join(",", values.keySet());
        String parameters = values.keySet().stream().map(this::parameter).collect(java.util.stream.Collectors.joining(","));
        jdbc.update("insert into evaluation_results(" + columns + ") values(" + parameters + ")", values);
    }
    private int update(EvaluationResult result, String condition) {
        var values = values(result);
        String setters = values.keySet().stream().filter(key -> !Set.of("id","test_execution_id","scan_id","test_case_id","category","created_at").contains(key))
                .map(key -> key + "=" + parameter(key)).collect(java.util.stream.Collectors.joining(","));
        return jdbc.update("update evaluation_results set " + setters + " where id=:id and " + condition, values);
    }
    private String parameter(String column) { return JSON_COLUMNS.contains(column) ? "cast(:" + column + " as jsonb)" : ":" + column; }
    private Map<String,Object> values(EvaluationResult r) {
        var v = new LinkedHashMap<String,Object>();
        put(v,"id",r.id(),"test_execution_id",r.testExecutionId(),"scan_id",r.scanId(),"test_case_id",r.securityTestId(),
                "category",r.category(),"status",r.status(),"created_at",r.createdAt(),"started_at",r.startedAt(),"finished_at",r.finishedAt(),
                "error_reason",r.errorReason(),"reasoning_summary",r.reasoningSummary(),"recommendation",r.recommendation(),"evidence_summary",r.evidenceSummary());
        var d = r.decision();
        put(v,"final_verdict",d==null?null:d.finalVerdict(),"deterministic_verdict",d==null?null:d.deterministicVerdict(),
                "severity",d==null?null:d.severity(),"confidence",d==null?null:d.confidence(),"confidence_components",d==null?null:EvaluationJson.write(d.confidenceComponents()),
                "decision_code",d==null?null:d.decisionCode(),"outcome_reason",d==null?null:d.outcomeReason(),"degraded",d!=null&&d.degraded(),
                "verdict_discrepancy",d!=null&&d.verdictDiscrepancy(),"severity_discrepancy",d!=null&&d.severityDiscrepancy());
        var llm = r.llm();
        put(v,"deterministic_signals",EvaluationJson.write(r.deterministicSignals()),"llm_status",llm.status(),"llm_error_reason",llm.errorReason(),
                "llm_attempt_count",llm.attemptCount(),"llm_assessment",llm.assessment()==null?null:EvaluationJson.write(llm.assessment()),
                "resolved_model",llm.resolvedModel(),"latency_ms",llm.latencyMs(),"input_tokens",llm.inputTokens(),"output_tokens",llm.outputTokens(),
                "reserved_cost_usd",llm.reservedCostUsd(),"estimated_cost_usd",llm.estimatedCostUsd(),"pricing_version",llm.pricingVersion(),
                "estimate_version",llm.estimateVersion(),"provider_request_id",llm.providerRequestId());
        var c = r.configuration();
        put(v,"criteria_id",c==null?null:c.criteriaId(),"criteria_version",c==null?null:c.criteriaVersion(),"scenario_id",c==null?null:c.scenarioId(),
                "scenario_version",c==null?null:c.scenarioVersion(),"engine_version",c==null?null:c.engineVersion(),"policy_version",c==null?null:c.policyVersion(),
                "confidence_version",c==null?null:c.confidenceVersion(),"severity_version",c==null?null:c.severityVersion(),"prompt_version",c==null?null:c.promptVersion(),
                "schema_version",c==null?null:c.schemaVersion(),"llm_input_profile",c==null?null:c.llmInputProfile(),"configuration_snapshot",c==null?null:c.configurationSnapshot(),
                "configuration_hash",c==null?null:c.configurationHash(),"requested_provider",c==null?null:c.requestedProvider(),"requested_model",c==null?null:c.requestedModel(),
                "sampling_parameters",c==null?null:c.samplingParameters());
        var e = r.evidence();
        put(v,"evidence_projection",e.projection(),"evidence_complete",e.complete(),"redacted",e.redacted(),"input_bytes",e.inputBytes(),
                "evidence_bytes",e.evidenceBytes(),"input_hash",e.inputHash());
        return v;
    }
    private void put(Map<String,Object> target, Object... pairs) {
        for (int i=0;i<pairs.length;i+=2) {
            Object value=pairs[i+1];
            target.put((String)pairs[i], value instanceof Enum<?> e ? e.name() : value instanceof Instant time ? time.atOffset(ZoneOffset.UTC) : value);
        }
    }
    private <T> T json(ResultSet rs, String name, Class<T> type) throws SQLException {
        String value=rs.getString(name); return value==null?null:EvaluationJson.MAPPER.readValue(value,type);
    }
    private <T extends Enum<T>> T enumeration(ResultSet rs,String name,Class<T> type) throws SQLException {
        String value=rs.getString(name); return value==null?null:Enum.valueOf(type,value);
    }
    private Instant time(ResultSet rs,String name) throws SQLException {
        OffsetDateTime value=rs.getObject(name,OffsetDateTime.class); return value==null?null:value.toInstant();
    }
    private EvaluationResult read(ResultSet rs,int row) throws SQLException {
        var d = rs.getString("final_verdict")==null?null:new HybridDecisionPolicy.Decision(
                enumeration(rs,"final_verdict",Verdict.class),enumeration(rs,"deterministic_verdict",Verdict.class),rs.getString("decision_code"),
                json(rs,"confidence_components",HybridDecisionPolicy.ConfidenceComponents.class),enumeration(rs,"severity",Severity.class),
                rs.getBoolean("degraded"),rs.getBoolean("verdict_discrepancy"),rs.getBoolean("severity_discrepancy"),enumeration(rs,"outcome_reason",OutcomeReason.class));
        var c=rs.getString("criteria_id")==null?null:new EvaluationResult.Configuration(rs.getString("criteria_id"),rs.getString("criteria_version"),
                rs.getString("scenario_id"),rs.getString("scenario_version"),rs.getString("engine_version"),rs.getString("policy_version"),rs.getString("confidence_version"),
                rs.getString("severity_version"),rs.getString("prompt_version"),rs.getString("schema_version"),rs.getString("llm_input_profile"),rs.getString("configuration_snapshot"),
                rs.getString("configuration_hash"),rs.getString("requested_provider"),rs.getString("requested_model"),rs.getString("sampling_parameters"));
        var llm = new EvaluationResult.LlmComponent(enumeration(rs,"llm_status",LlmStatus.class),enumeration(rs,"llm_error_reason",LlmError.class),
                rs.getInt("llm_attempt_count"),json(rs,"llm_assessment",LlmAssessment.class),rs.getString("resolved_model"),rs.getObject("latency_ms",Long.class),
                rs.getObject("input_tokens",Long.class),rs.getObject("output_tokens",Long.class),rs.getBigDecimal("reserved_cost_usd"),rs.getBigDecimal("estimated_cost_usd"),
                rs.getString("pricing_version"),rs.getString("estimate_version"),rs.getString("provider_request_id"));
        var evidence=new EvaluationResult.Evidence(rs.getString("evidence_projection"),rs.getBoolean("evidence_complete"),rs.getBoolean("redacted"),
                rs.getObject("input_bytes",Integer.class),rs.getObject("evidence_bytes",Integer.class),rs.getString("input_hash"));
        return new EvaluationResult(rs.getObject("id",UUID.class),rs.getObject("test_execution_id",UUID.class),rs.getObject("scan_id",UUID.class),rs.getObject("test_case_id",UUID.class),
                enumeration(rs,"category",SecurityCategory.class),enumeration(rs,"status",EvaluationStatus.class),time(rs,"created_at"),time(rs,"started_at"),time(rs,"finished_at"),
                enumeration(rs,"error_reason",EvaluationError.class),d,EvaluationJson.readSignals(rs.getString("deterministic_signals")),llm,c,evidence,
                rs.getString("reasoning_summary"),rs.getString("recommendation"),rs.getString("evidence_summary"));
    }
    @Override public EvaluationListing listEvaluations(UUID scanId) {
        try {
            var rows=jdbc.query("""
                    select r.id,r.scan_id,r.test_execution_id,t.code,t.revision,r.category,r.status,r.final_verdict,r.confidence,r.severity,
                    r.llm_status,r.llm_error_reason,r.requested_provider,r.requested_model,r.criteria_id,r.criteria_version,r.decision_code,
                    r.degraded,r.evidence_complete,r.outcome_reason,r.error_reason,r.reasoning_summary,r.recommendation,r.started_at,r.finished_at,f.id finding_id
                    from evaluation_results r join test_cases t on t.id=r.test_case_id left join findings f on f.evaluation_result_id=r.id
                    where r.scan_id=:id order by t.code,t.revision,r.id
                    """,Map.of("id",scanId),(rs,n)->new EvaluationMetadata(rs.getObject("id",UUID.class),rs.getObject("scan_id",UUID.class),rs.getObject("test_execution_id",UUID.class),
                        rs.getString("code"),rs.getInt("revision"),enumeration(rs,"category",SecurityCategory.class),enumeration(rs,"status",EvaluationStatus.class),
                        enumeration(rs,"final_verdict",Verdict.class),rs.getBigDecimal("confidence"),enumeration(rs,"severity",Severity.class),enumeration(rs,"llm_status",LlmStatus.class),
                        enumeration(rs,"llm_error_reason",LlmError.class),rs.getString("requested_provider"),rs.getString("requested_model"),rs.getString("criteria_id"),rs.getString("criteria_version"),
                        rs.getString("decision_code"),rs.getBoolean("degraded"),rs.getBoolean("evidence_complete"),enumeration(rs,"outcome_reason",OutcomeReason.class),
                        enumeration(rs,"error_reason",EvaluationError.class),rs.getString("reasoning_summary"),rs.getString("recommendation"),time(rs,"started_at"),time(rs,"finished_at"),rs.getObject("finding_id",UUID.class)));
            return new EvaluationListing(profile(scanId),rows);
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    @Override public List<FindingMetadata> listFindings(UUID scanId) {
        try {
            return jdbc.query("""
                    select f.id,f.evaluation_result_id,f.scan_id,f.test_execution_id,t.code,t.revision,f.owasp_category,
                    f.final_verdict,f.risk_level,f.confidence,f.title,f.explanation,f.evidence,f.recommendation,f.created_at
                    from findings f join test_cases t on t.id=f.test_case_id where f.scan_id=:id order by f.created_at,f.id
                    """,Map.of("id",scanId),(rs,n)->new FindingMetadata(rs.getObject("id",UUID.class),rs.getObject("evaluation_result_id",UUID.class),rs.getObject("scan_id",UUID.class),
                        rs.getObject("test_execution_id",UUID.class),rs.getString("code"),rs.getInt("revision"),enumeration(rs,"owasp_category",SecurityCategory.class),
                        Verdict.VULNERABLE,enumeration(rs,"risk_level",Severity.class),rs.getBigDecimal("confidence"),rs.getString("title"),rs.getString("explanation"),
                        rs.getString("evidence"),rs.getString("recommendation"),time(rs,"created_at")));
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
}
