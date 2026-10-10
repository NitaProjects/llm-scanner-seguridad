package com.llmscannerseguridad.adapters.out.persistence.reporting;

import java.sql.*;
import java.time.ZoneOffset;
import java.util.*;
import javax.sql.DataSource;
import com.llmscannerseguridad.adapters.reporting.ReportJson;
import com.llmscannerseguridad.adapters.out.reporting.LocalReportPresentationAdapter;
import com.llmscannerseguridad.application.reporting.*;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.reporting.*;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import static com.llmscannerseguridad.adapters.out.persistence.reporting.ReportRows.*;

public final class PostgresSecurityReportRepositoryAdapter implements SecurityReportRepositoryPort {
    private final NamedParameterJdbcTemplate jdbc;
    private final LocalReportPresentationAdapter presentation = new LocalReportPresentationAdapter();
    public PostgresSecurityReportRepositoryAdapter(DataSource source) { jdbc = new NamedParameterJdbcTemplate(source); }
    @Override public void setRequiredVersion(UUID scanId) {
        participating();
        if (jdbc.update("""
                update scans set report_version='report-v1' where id=:id and status='PENDING' and report_version is null
                    and scoring_version='scoring-v1' and evaluation_profile_version='evaluation-engine-v1'
                """,Map.of("id",scanId)) != 1) throw ScanOperationException.internal();
    }
    @Override public ReportSnapshot lock(UUID scanId) {
        participating();
        if (jdbc.queryForList("select id from scans where id=:id for update",Map.of("id",scanId),UUID.class).size() != 1) throw ScanOperationException.internal();
        var rows = jdbc.query(SNAPSHOT+" where s.id=:id",Map.of("id",scanId),this::read);
        if (rows.size() != 1) throw ScanOperationException.internal();
        return rows.getFirst();
    }
    @Override public Optional<ReportSnapshot> findScoped(UUID projectId, UUID scanId) {
        try {
            var rows = jdbc.query(SNAPSHOT+" where s.id=:id and s.project_id=:project",Map.of("id",scanId,"project",projectId),this::read);
            if (rows.size() > 1) throw ScanOperationException.internal();
            return rows.stream().findFirst();
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    @Override public SecurityReport insert(SecurityReport candidate) {
        try {
            participating(); var prior = lock(candidate.scanId());
            if (prior.report() != null) {
                prior.validate();
                if (!prior.report().sameContent(candidate)) throw ScanOperationException.internal();
                return prior.report();
            }
            // Estado interno de T3: el score insertado todavía no es un resultado durable público.
            ReportRules.require(prior.scan().status() == ScanStatus.RUNNING && ReportRules.VERSION.equals(prior.reportVersion())
                    && "scoring-v1".equals(prior.scoringVersion()) && "evaluation-engine-v1".equals(prior.evaluationProfile())
                    && candidate.projectId().equals(prior.scan().projectId()) && candidate.summary().equals(prior.summary())
                    && candidate.scope().scanCreatedAt().equals(prior.scan().createdAt())
                    && candidate.scope().scanStartedAt().equals(prior.scan().startedAt()));
            presentation.validate(candidate); var json = ReportJson.write(candidate);
            var p = new HashMap<String,Object>();
            p.put("report",candidate.reportId()); p.put("scan",candidate.scanId()); p.put("version",candidate.reportVersion());
            p.put("score",candidate.summary().scoringVersion()); p.put("at",candidate.generatedAt().atOffset(ZoneOffset.UTC));
            p.put("locale",candidate.locale()); p.put("payload",json);
            if (jdbc.update("""
                    insert into security_reports(report_id,scan_id,report_version,scoring_version,generated_at,locale,payload)
                    values(:report,:scan,:version,:score,:at,:locale,cast(:payload as jsonb))
                    """,p) != 1) throw ScanOperationException.internal();
            return candidate;
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    @Override public void verifyRecoveryIntegrity() {
        participating();
        jdbc.query(SNAPSHOT+" where s.report_version is not null or r.report_id is not null or s.scoring_version is not null or q.scan_id is not null order by s.id",
                Map.of(),this::read).forEach(ReportSnapshot::validate);
    }
    private ReportSnapshot read(ResultSet r, int index) throws SQLException {
        SecurityReport report = null;
        if (uuid(r,"report_id") != null) {
            report = ReportJson.read(r.getString("payload"),SecurityReport.class); presentation.validate(report);
            ReportRules.require(report.reportId().equals(uuid(r,"report_id")) && report.scanId().equals(uuid(r,"report_scan_id"))
                    && report.reportVersion().equals(r.getString("report_version")) && report.summary().scoringVersion().equals(r.getString("report_scoring_version"))
                    && report.generatedAt().equals(time(r,"generated_at")) && report.locale().equals(r.getString("locale")));
        }
        return new ReportSnapshot(scan(r),r.getString("required_report"),r.getString("required_score"),r.getString("evaluation_profile_version"),summary(r),report);
    }
}
