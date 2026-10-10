package com.llmscannerseguridad.adapters.out.persistence.scoring;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import com.llmscannerseguridad.application.scoring.ScoringInputPort;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.execution.ExecutionStatus;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.llmscannerseguridad.application.scan.ScanOperationException;

public final class PostgresScoringInputAdapter implements ScoringInputPort {
    private final NamedParameterJdbcTemplate jdbc;
    public PostgresScoringInputAdapter(DataSource source) { jdbc = new NamedParameterJdbcTemplate(source); }
    @Override public Inputs read(UUID scanId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw ScanOperationException.internal();
        var parameters = Map.of("id", scanId);
        // Sin DISTINCT, límites ni columnas de evidencia; una relación ausente no desaparece por un inner join.
        var executions = jdbc.query("""
                select e.id,e.scan_id,e.test_case_id,t.code,t.revision,t.category,e.status,e.finished_at
                from test_executions e left join test_cases t on t.id=e.test_case_id where e.scan_id=:id order by e.id
                """, parameters, (r, n) -> new Execution(uuid(r,"id"),uuid(r,"scan_id"),uuid(r,"test_case_id"),
                    r.getString("code"),r.getInt("revision"),enumValue(SecurityCategory.class,r.getString("category")),
                    enumValue(ExecutionStatus.class,r.getString("status")),time(r,"finished_at")));
        var evaluations = jdbc.query("""
                select id,scan_id,test_execution_id,test_case_id,category,status,final_verdict,severity,confidence,finished_at
                from evaluation_results where scan_id=:id order by id
                """, parameters, (r, n) -> new Evaluation(uuid(r,"id"),uuid(r,"scan_id"),uuid(r,"test_execution_id"),uuid(r,"test_case_id"),
                    enumValue(SecurityCategory.class,r.getString("category")),enumValue(EvaluationStatus.class,r.getString("status")),
                    enumValue(Verdict.class,r.getString("final_verdict")),enumValue(Severity.class,r.getString("severity")),
                    r.getBigDecimal("confidence"),time(r,"finished_at")));
        var findings = jdbc.query("""
                select id,scan_id,test_execution_id,test_case_id,evaluation_result_id,owasp_category,final_verdict,risk_level,confidence,created_at
                from findings where scan_id=:id order by id
                """, parameters, (r, n) -> new Finding(uuid(r,"id"),uuid(r,"scan_id"),uuid(r,"test_execution_id"),uuid(r,"test_case_id"),
                    uuid(r,"evaluation_result_id"),enumValue(SecurityCategory.class,r.getString("owasp_category")),enumValue(Verdict.class,r.getString("final_verdict")),
                    enumValue(Severity.class,r.getString("risk_level")),r.getBigDecimal("confidence"),time(r,"created_at")));
        return new Inputs(executions, evaluations, findings);
    }
    static UUID uuid(ResultSet row, String column) throws SQLException { return row.getObject(column, UUID.class); }
    static Instant time(ResultSet row, String column) throws SQLException {
        var value = row.getTimestamp(column); return value == null ? null : value.toInstant();
    }
    static <E extends Enum<E>> E enumValue(Class<E> type, String value) { return value == null ? null : Enum.valueOf(type, value); }
}
