package com.llmscannerseguridad.adapters.out.persistence.scoring;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.*;
import javax.sql.DataSource;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.application.scoring.*;
import com.llmscannerseguridad.domain.scan.*;
import com.llmscannerseguridad.domain.scoring.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.llmscannerseguridad.adapters.out.persistence.scoring.PostgresScoringInputAdapter.*;

public final class PostgresScanScoreRepositoryAdapter implements ScanScoreRepositoryPort {
    private static final String SNAPSHOT = """
            select s.id,s.project_id,s.status,s.created_at,s.started_at,s.finished_at,s.error_reason,
                s.scoring_version as required_version,s.evaluation_profile_version,
                r.scan_id as score_scan_id,r.scoring_version,r.calculated_at,r.global_score,r.risk_level,
                r.evaluation_count,r.finding_count,r.not_vulnerable_count,r.inconclusive_count,
                r.severity_counts::text,r.categories::text
            from scans s left join scan_scores r on r.scan_id=s.id
            """;
    private final NamedParameterJdbcTemplate jdbc;
    public PostgresScanScoreRepositoryAdapter(DataSource source) { jdbc = new NamedParameterJdbcTemplate(source); }
    private void participating() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw ScanOperationException.internal();
    }
    @Override public void setRequiredVersion(UUID scanId) {
        participating();
        if (jdbc.update("""
                update scans set scoring_version=:version where id=:id and status='PENDING'
                    and evaluation_profile_version='evaluation-engine-v1' and scoring_version is null
                """, Map.of("id",scanId,"version",ScoringPolicyV1.VERSION)) != 1) throw ScanOperationException.internal();
    }
    @Override public ScanScoreSnapshot lock(UUID scanId) {
        participating();
        // Lock primero. El SELECT siguiente usa una instantánea posterior a cualquier cierre que esperásemos.
        var ids = jdbc.queryForList("select id from scans where id=:id for update",Map.of("id",scanId),UUID.class);
        if (ids.size() != 1) throw ScanOperationException.internal();
        var rows = jdbc.query(SNAPSHOT + " where s.id=:id",Map.of("id",scanId),this::read);
        if (rows.size() != 1) throw ScanOperationException.internal();
        return rows.getFirst();
    }
    @Override public Optional<ScanScoreSnapshot> findScoped(UUID projectId, UUID scanId) {
        try {
            var rows = jdbc.query(SNAPSHOT + " where s.id=:id and s.project_id=:project",Map.of("id",scanId,"project",projectId),this::read);
            if (rows.size() > 1) throw ScanOperationException.internal();
            return rows.stream().findFirst();
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    @Override public ScanScore insert(ScanScore candidate) {
        participating();
        com.llmscannerseguridad.privacy.ProtectionScope.requireScan(candidate.scanId());
        com.llmscannerseguridad.privacy.DurableEvidenceProtection.requireDocument(ScoringJson.write(candidate), 65536);
        var snapshot = lock(candidate.scanId());
        snapshot.validate();
        if (snapshot.score() != null) {
            if (!snapshot.score().sameResult(candidate)) throw ScanOperationException.internal();
            return snapshot.score();
        }
        if (snapshot.scan().status() != ScanStatus.RUNNING || !candidate.scoringVersion().equals(snapshot.requiredVersion())
                || candidate.calculatedAt().isBefore(snapshot.scan().startedAt())) throw ScanOperationException.internal();
        var parameters = new HashMap<String,Object>();
        parameters.put("id",candidate.scanId()); parameters.put("version",candidate.scoringVersion());
        parameters.put("at",candidate.calculatedAt().atOffset(ZoneOffset.UTC)); parameters.put("score",candidate.globalScore());
        parameters.put("level",candidate.riskLevel().name()); parameters.put("evaluations",candidate.evaluationCount());
        parameters.put("findings",candidate.findingCount()); parameters.put("negatives",candidate.notVulnerableCount());
        parameters.put("inconclusives",candidate.inconclusiveCount()); parameters.put("severities",ScoringJson.write(candidate.severityCounts()));
        parameters.put("categories",ScoringJson.write(candidate.categories()));
        if (jdbc.update("""
                insert into scan_scores(scan_id,scoring_version,calculated_at,global_score,risk_level,evaluation_count,
                    finding_count,not_vulnerable_count,inconclusive_count,severity_counts,categories)
                values(:id,:version,:at,:score,:level,:evaluations,:findings,:negatives,:inconclusives,
                    cast(:severities as jsonb),cast(:categories as jsonb))
                """,parameters) != 1) throw ScanOperationException.internal();
        return candidate;
    }
    @Override public void verifyRecoveryIntegrity() {
        participating();
        var rows = jdbc.query(SNAPSHOT + " where s.scoring_version is not null or r.scan_id is not null order by s.id",Map.of(),this::read);
        rows.forEach(ScanScoreSnapshot::validate);
    }
    private ScanScoreSnapshot read(ResultSet r, int index) throws SQLException {
        var scan = new Scan(uuid(r,"id"),uuid(r,"project_id"),ScanStatus.valueOf(r.getString("status")),
                time(r,"created_at"),time(r,"started_at"),time(r,"finished_at"),enumValue(ScanFailureReason.class,r.getString("error_reason")));
        var score = uuid(r,"score_scan_id") == null ? null : new ScanScore(uuid(r,"score_scan_id"),r.getString("scoring_version"),
                time(r,"calculated_at"),r.getInt("global_score"),RiskLevel.valueOf(r.getString("risk_level")),r.getInt("evaluation_count"),
                r.getInt("finding_count"),r.getInt("not_vulnerable_count"),r.getInt("inconclusive_count"),
                ScoringJson.severities(r.getString("severity_counts")),ScoringJson.categories(r.getString("categories")));
        return new ScanScoreSnapshot(scan,r.getString("required_version"),r.getString("evaluation_profile_version"),score);
    }
}
