package com.llmscannerseguridad.adapters.out.persistence.reporting;

import java.util.*;
import javax.sql.DataSource;
import com.llmscannerseguridad.application.reporting.ReportInputPort;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.execution.ExecutionStatus;
import com.llmscannerseguridad.domain.reporting.*;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import static com.llmscannerseguridad.adapters.out.persistence.reporting.ReportRows.*;

public final class PostgresReportInputAdapter implements ReportInputPort {
    private final NamedParameterJdbcTemplate jdbc;
    public PostgresReportInputAdapter(DataSource source) { jdbc = new NamedParameterJdbcTemplate(source); }
    @Override public Inputs read(UUID scanId) {
        try {
            participating(); var p = Map.of("id",scanId);
            var head = jdbc.query(SNAPSHOT + " where s.id=:id",p,(r,n) -> {
                if (uuid(r,"report_id") != null) throw ScanOperationException.internal();
                return new Inputs(scan(r),r.getString("required_report"),r.getString("required_score"),
                        r.getString("evaluation_profile_version"),summary(r),List.of(),List.of(),List.of());
            });
            if (head.size() != 1) throw ScanOperationException.internal();
            var executions = jdbc.query("""
                    select x.id,x.scan_id,x.test_case_id,t.code,t.revision,t.category,x.status,x.finished_at,
                        x.target_configuration_id,x.target_updated_at
                    from test_executions x left join test_cases t on t.id=x.test_case_id where x.scan_id=:id order by x.id
                    """,p,(r,n) -> new Execution(uuid(r,"id"),uuid(r,"scan_id"),uuid(r,"test_case_id"),r.getString("code"),r.getInt("revision"),
                        value(SecurityCategory.class,r.getString("category")),value(ExecutionStatus.class,r.getString("status")),
                        time(r,"finished_at"),uuid(r,"target_configuration_id"),time(r,"target_updated_at")));
            // Lista positiva; no se hidrata EvaluationResult, assessment, evidence ni configuración.
            var evaluations = jdbc.query("""
                    select e.id,e.scan_id,e.test_execution_id,e.test_case_id,t.code,t.revision,e.category,e.status,e.final_verdict,
                        e.confidence,e.severity,e.outcome_reason,e.llm_status,e.llm_error_reason,e.degraded,e.evidence_complete,
                        e.criteria_id,e.criteria_version,e.engine_version,e.policy_version,e.confidence_version,e.severity_version,
                        e.prompt_version,e.schema_version,e.llm_input_profile,e.finished_at,f.id as finding_id
                    from evaluation_results e left join test_cases t on t.id=e.test_case_id
                        left join findings f on f.evaluation_result_id=e.id where e.scan_id=:id order by e.id
                    """,p,(r,n) -> new Evaluation(uuid(r,"scan_id"),uuid(r,"test_case_id"),new ReportEvaluation(uuid(r,"id"),uuid(r,"test_execution_id"),
                        r.getString("code"),r.getInt("revision"),value(SecurityCategory.class,r.getString("category")),
                        value(EvaluationStatus.class,r.getString("status")),value(Verdict.class,r.getString("final_verdict")),r.getBigDecimal("confidence"),
                        value(Severity.class,r.getString("severity")),value(OutcomeReason.class,r.getString("outcome_reason")),
                        value(LlmStatus.class,r.getString("llm_status")),value(LlmError.class,r.getString("llm_error_reason")),
                        r.getBoolean("degraded"),r.getBoolean("evidence_complete"),r.getString("criteria_id"),r.getString("criteria_version"),
                        new ReportEvaluation.Versions(r.getString("engine_version"),r.getString("policy_version"),r.getString("confidence_version"),
                            r.getString("severity_version"),r.getString("prompt_version"),r.getString("schema_version"),r.getString("llm_input_profile")),
                        time(r,"finished_at"),uuid(r,"finding_id"))));
            var findings = jdbc.query("""
                    select f.id,f.evaluation_result_id,f.scan_id,f.test_execution_id,f.test_case_id,t.code,t.revision,f.owasp_category,
                        f.final_verdict,f.risk_level,f.confidence,f.title,f.explanation,f.evidence,f.recommendation,f.created_at
                    from findings f left join test_cases t on t.id=f.test_case_id where f.scan_id=:id order by f.id
                    """,p,(r,n) -> new Finding(uuid(r,"test_case_id"),new ReportFinding(uuid(r,"id"),uuid(r,"evaluation_result_id"),uuid(r,"scan_id"),
                        uuid(r,"test_execution_id"),r.getString("code"),r.getInt("revision"),value(SecurityCategory.class,r.getString("owasp_category")),
                        value(Verdict.class,r.getString("final_verdict")),value(Severity.class,r.getString("risk_level")),r.getBigDecimal("confidence"),
                        r.getString("title"),r.getString("explanation"),r.getString("evidence"),r.getString("recommendation"),time(r,"created_at"))));
            var h = head.getFirst();
            return new Inputs(h.scan(),h.reportVersion(),h.scoringVersion(),h.evaluationProfile(),h.summary(),executions,evaluations,findings);
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
}
