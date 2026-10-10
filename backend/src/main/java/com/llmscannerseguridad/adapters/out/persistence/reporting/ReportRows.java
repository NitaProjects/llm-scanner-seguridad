package com.llmscannerseguridad.adapters.out.persistence.reporting;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import com.llmscannerseguridad.adapters.reporting.ReportJson;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.scan.*;
import com.llmscannerseguridad.domain.reporting.*;
import com.llmscannerseguridad.domain.scoring.RiskLevel;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class ReportRows {
    static final String SNAPSHOT = """
            select s.id,s.project_id,s.status,s.created_at,s.started_at,s.finished_at,s.error_reason,
                s.report_version as required_report,s.scoring_version as required_score,s.evaluation_profile_version,
                q.scan_id as score_scan_id,q.scoring_version,q.calculated_at,q.global_score,q.risk_level,q.evaluation_count,
                q.finding_count,q.not_vulnerable_count,q.inconclusive_count,q.severity_counts::text,q.categories::text,
                r.report_id,r.scan_id as report_scan_id,r.report_version,r.scoring_version as report_scoring_version,
                r.generated_at,r.locale,r.payload::text
            from scans s left join scan_scores q on q.scan_id=s.id left join security_reports r on r.scan_id=s.id
            """;
    static void participating() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw ScanOperationException.internal();
    }
    static UUID uuid(ResultSet r, String name) throws SQLException { return r.getObject(name,UUID.class); }
    static Instant time(ResultSet r, String name) throws SQLException {
        var t = r.getTimestamp(name); return t == null ? null : t.toInstant();
    }
    static <T extends Enum<T>> T value(Class<T> type, String text) { return text == null ? null : Enum.valueOf(type,text); }
    static Scan scan(ResultSet r) throws SQLException {
        return new Scan(uuid(r,"id"),uuid(r,"project_id"),value(ScanStatus.class,r.getString("status")),
                time(r,"created_at"),time(r,"started_at"),time(r,"finished_at"),value(ScanFailureReason.class,r.getString("error_reason")));
    }
    static ReportSummary summary(ResultSet r) throws SQLException {
        if (uuid(r,"score_scan_id") == null) return null;
        return new ReportSummary(uuid(r,"score_scan_id"),r.getString("scoring_version"),time(r,"calculated_at"),r.getInt("global_score"),
                value(RiskLevel.class,r.getString("risk_level")),r.getInt("evaluation_count"),r.getInt("finding_count"),
                r.getInt("not_vulnerable_count"),r.getInt("inconclusive_count"),ReportJson.read(r.getString("severity_counts"),ReportSummary.Counts.class),
                List.of(ReportJson.read(r.getString("categories"),ReportSummary.Category[].class)));
    }
    private ReportRows() { }
}
