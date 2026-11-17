package com.llmscannerseguridad.adapters.out.persistence.project;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.application.evaluation.EphemeralEvaluationCorpus;
import com.llmscannerseguridad.application.project.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresProjectDeletionAdapter implements ProjectDeletionPort {
    private static final List<String> SCAN_CHILDREN = List.of("security_reports", "scan_scores", "findings",
            "evaluation_results", "test_executions");
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final TransactionTemplate transaction;
    private final EphemeralEvaluationCorpus corpus;

    public PostgresProjectDeletionAdapter(JdbcTemplate jdbc, PlatformTransactionManager manager, EphemeralEvaluationCorpus corpus) {
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc);
        this.corpus = corpus;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public void delete(UUID projectId) {
        try {
            transaction.executeWithoutResult(status -> remove(projectId));
        } catch (ProjectNotFoundException | ProjectDeletionException error) {
            throw error;
        } catch (RuntimeException error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && "23503".equals(sql.getSQLState())) {
                    throw new ProjectDeletionException(ProjectDeletionException.Reason.PROJECT_DELETE_CONFLICT);
                }
            }
            throw new ProjectPersistenceException(error);
        }
    }

    private void remove(UUID projectId) {
        jdbc.execute("set local lock_timeout = '5s'");
        if (jdbc.queryForList("select id from projects where id = ? for update", UUID.class, projectId).isEmpty()) {
            throw new ProjectNotFoundException();
        }
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from scans where project_id = ? and status in ('PENDING', 'RUNNING'))
                """, Boolean.class, projectId))) {
            throw new ProjectDeletionException(ProjectDeletionException.Reason.SCAN_ALREADY_ACTIVE);
        }
        var retained = new ArrayList<>(corpus.retainedIds());
        var release = new ArrayList<UUID>();
        for (int start = 0; start < retained.size(); start += 256) {
            release.addAll(named.queryForList("""
                    select e.id from evaluation_results e join scans s on s.id = e.scan_id
                    where s.project_id = :project and e.id in (:ids)
                    """, java.util.Map.of("project", projectId, "ids", retained.subList(start, Math.min(start + 256, retained.size()))),
                    UUID.class));
        }
        for (String table : SCAN_CHILDREN) {
            jdbc.update("delete from " + table + " where scan_id in (select id from scans where project_id = ?)", projectId);
        }
        jdbc.update("delete from scans where project_id = ?", projectId);
        jdbc.update("delete from target_configurations where project_id = ?", projectId);
        if (jdbc.update("delete from projects where id = ?", projectId) != 1) {
            throw new IllegalStateException("La fila del proyecto bloqueado no se pudo eliminar.");
        }
        if (!release.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    for (UUID id : release) {
                        try {
                            corpus.release(id);
                        } catch (RuntimeException error) {
                            org.slf4j.LoggerFactory.getLogger(PostgresProjectDeletionAdapter.class)
                                    .warn("PROJECT_CORPUS_RELEASE_FAILED");
                        }
                    }
                }
            });
        }
    }
}
