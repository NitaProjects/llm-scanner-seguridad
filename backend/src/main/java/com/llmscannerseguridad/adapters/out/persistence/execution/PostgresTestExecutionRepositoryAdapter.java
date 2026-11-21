package com.llmscannerseguridad.adapters.out.persistence.execution;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.application.execution.ExecutionMetadata;
import com.llmscannerseguridad.application.execution.ExecutionPlan;
import com.llmscannerseguridad.application.execution.InvocationResult;
import com.llmscannerseguridad.application.execution.SecurityTestCatalogPort;
import com.llmscannerseguridad.application.execution.TestExecutionRepositoryPort;
import com.llmscannerseguridad.application.execution.TargetIntegrationPort;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.application.scan.ScanRepositoryPort;
import com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.execution.ExecutionStatus;
import com.llmscannerseguridad.domain.execution.TestExecution;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public class PostgresTestExecutionRepositoryAdapter implements TestExecutionRepositoryPort {
    private final EntityManager entityManager;
    private final ScanRepositoryPort scans;
    private final TargetConfigurationRepositoryPort targets;
    private final SecurityTestCatalogPort catalog;
    private final ExecutionLimits limits;
    private final Clock clock;
    private final TargetIntegrationPort integrations;
    private final TransactionTemplate transaction;
    private final com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort evaluations;

    public PostgresTestExecutionRepositoryAdapter(EntityManager entityManager, ScanRepositoryPort scans,
            TargetConfigurationRepositoryPort targets, SecurityTestCatalogPort catalog, ExecutionLimits limits,
            PlatformTransactionManager manager, Clock clock) {
        this(entityManager, scans, targets, catalog, limits, manager, clock, null);
    }

    public PostgresTestExecutionRepositoryAdapter(EntityManager entityManager, ScanRepositoryPort scans,
            TargetConfigurationRepositoryPort targets, SecurityTestCatalogPort catalog, ExecutionLimits limits,
            PlatformTransactionManager manager, Clock clock,
            com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort evaluations) {
        this(entityManager, scans, targets, catalog, limits, manager, clock, evaluations, target -> null);
    }

    public PostgresTestExecutionRepositoryAdapter(EntityManager entityManager, ScanRepositoryPort scans,
            TargetConfigurationRepositoryPort targets, SecurityTestCatalogPort catalog, ExecutionLimits limits,
            PlatformTransactionManager manager, Clock clock,
            com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort evaluations, TargetIntegrationPort integrations) {
        this.entityManager = entityManager;
        this.scans = scans;
        this.targets = targets;
        this.catalog = catalog;
        this.limits = limits;
        this.clock = clock;
        this.evaluations = evaluations;
        this.integrations = integrations;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private <T> T confirmed(Supplier<T> operation) {
        try {
            return transaction.execute(status -> operation.get());
        } catch (RuntimeException error) {
            throw ScanOperationException.internal();
        }
    }

    @Override
    public ExecutionPlan prepare(UUID scanId) {
        com.llmscannerseguridad.privacy.ProtectionScope.limitTo(limits.scanEvidenceBytes());
        return confirmed(() -> {
            var scan = scans.findById(scanId).orElseThrow(ScanOperationException::internal);
            if (scan.status() != ScanStatus.RUNNING || !findByScanId(scanId).isEmpty()) {
                throw ScanOperationException.internal();
            }
            var tests = catalog.findAllOrdered();
            if (tests.size() > limits.maxTests()) {
                throw ScanOperationException.internal();
            }
            var target = targets.findByProjectId(scan.projectId()).orElseThrow(ScanOperationException::internal);
            var integration = integrations.resolve(target);
            var rows = tests.stream().map(test -> TestExecution.pending(UUID.randomUUID(), scanId, test.id(),
                    target.id(), target.updatedAt())).toList();
            var plan = new ExecutionPlan(tests, rows, target, scan.startedAt(), integration);
            rows.forEach(row -> entityManager.persist(new TestExecutionJpaEntity(row)));
            entityManager.flush();
            if (evaluations != null && evaluations.profile(scanId) != null) {
                var evaluationItems = java.util.stream.IntStream.range(0, tests.size()).mapToObj(i ->
                        new com.llmscannerseguridad.application.evaluation.EvaluationPlan.Item(rows.get(i).id(), tests.get(i).id(),
                            tests.get(i).code(), tests.get(i).revision(), tests.get(i).category())).toList();
                evaluations.prepare(new com.llmscannerseguridad.application.evaluation.EvaluationPlan(scan.projectId(), scan.id(),
                        target.id(), target.updatedAt(), scan.startedAt(), evaluationItems));
            }
            return plan;
        });
    }

    @Override
    public TestExecution start(UUID executionId) {
        var started = confirmed(() -> {
            var entity = entityManager.find(TestExecutionJpaEntity.class, executionId, LockModeType.PESSIMISTIC_WRITE);
            if (entity == null) {
                throw ScanOperationException.internal();
            }
            var row = entity.toDomain();
            long storedBytes = findByScanId(row.scanId()).stream().mapToLong(value ->
                    bytes(value.requestBody()) + bytes(TestExecutionJpaEntity.encodeResponse(value.responseText()))).sum();
            var scan = scans.findById(row.scanId()).orElseThrow(ScanOperationException::internal);
            if (scan.status() != ScanStatus.RUNNING) {
                throw ScanOperationException.internal();
            }
            var running = row.start(clock.instant(), scan.startedAt());
            entity.replace(running);
            entityManager.flush();
            return new StartedExecution(running, storedBytes >= limits.scanEvidenceBytes());
        });
        if (started.evidenceExhausted()) {
            // Confirmar la causa técnica antes de que T3 aborte las pruebas restantes.
            finish(started.execution(), InvocationResult.failure(ExecutionError.EVIDENCE_LIMIT_EXCEEDED));
            throw ScanOperationException.internal();
        }
        return started.execution();
    }

    private record StartedExecution(TestExecution execution, boolean evidenceExhausted) {
    }

    @Override
    public void finish(TestExecution running, InvocationResult candidate) {
        com.llmscannerseguridad.privacy.ProtectionScope.requireScan(running.scanId());
        var result = new com.llmscannerseguridad.privacy.DurableEvidenceProtection(
                limits.requestBytes(), limits.textBytes(), limits.evidenceBytes()).protect(candidate, null);
        boolean exhausted = confirmed(() -> {
            var entity = entityManager.find(TestExecutionJpaEntity.class, running.id(), LockModeType.PESSIMISTIC_WRITE);
            if (entity == null || entity.toDomain().status() != ExecutionStatus.RUNNING) {
                throw ScanOperationException.internal();
            }
            long bytes = bytes(result.requestBody()) + bytes(TestExecutionJpaEntity.encodeResponse(result.responseText()));
            long accumulated = findByScanId(running.scanId()).stream().mapToLong(row ->
                    bytes(row.requestBody()) + bytes(TestExecutionJpaEntity.encodeResponse(row.responseText()))).sum();
            boolean global = accumulated + bytes > limits.scanEvidenceBytes();
            var safe = result;
            if (global || bytes > limits.evidenceBytes() || bytes(result.requestBody()) > limits.requestBytes()) {
                safe = new InvocationResult(result.httpStatus(), result.latencyMs(), ExecutionError.EVIDENCE_LIMIT_EXCEEDED,
                        null, null, false, false);
            }
            entity.replace(running.finish(clock.instant(), safe.httpStatus(), safe.latencyMs(), safe.errorReason(),
                    safe.requestBody(), safe.responseText(), safe.requestRedacted(), safe.responseRedacted()));
            entityManager.flush();
            return global;
        });
        if (exhausted) {
            // El FAILED está confirmado; el resto debe cerrarse junto con Scan en T3.
            throw ScanOperationException.internal();
        }
    }

    private static int bytes(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestExecution> findByScanId(UUID scanId) {
        try {
            return entityManager.createQuery("select e from TestExecutionJpaEntity e where e.scanId = :scan", TestExecutionJpaEntity.class)
                    .setParameter("scan", scanId).getResultList().stream().map(TestExecutionJpaEntity::toDomain).toList();
        } catch (RuntimeException error) {
            throw ScanOperationException.internal();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExecutionMetadata> listMetadata(UUID scanId) {
        try {
            // Proyección: no lee los cuerpos de evidencia ni la configuración del target.
            return entityManager.createQuery("""
                    select new com.llmscannerseguridad.application.execution.ExecutionMetadata(
                        e.id, e.scanId, t.code, t.revision, t.name, t.category, e.status, e.startedAt,
                        e.finishedAt, e.httpStatus, e.latencyMs, e.errorReason, e.requestRedacted, e.responseRedacted)
                    from TestExecutionJpaEntity e, SecurityTestJpaEntity t
                    where e.scanId = :scan and e.securityTestId = t.id order by t.code, t.revision
                    """, ExecutionMetadata.class).setParameter("scan", scanId).getResultList();
        } catch (RuntimeException error) {
            throw ScanOperationException.internal();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean allCompleted(UUID scanId) {
        var rows = listMetadata(scanId);
        return rows.size() == 4 && rows.stream().allMatch(row -> row.status() == ExecutionStatus.COMPLETED);
    }

    @Override
    public void closeActive(UUID scanId, Instant now, ExecutionError reason) {
        requireTransaction();
        var scan = scans.findById(scanId).orElseThrow(ScanOperationException::internal);
        for (var row : findByScanId(scanId)) {
            if (row.status().active()) {
                var entity = entityManager.find(TestExecutionJpaEntity.class, row.id(), LockModeType.PESSIMISTIC_WRITE);
                var current = entity.toDomain();
                if (current.status().active()) {
                    entity.replace(current.abort(now, scan.startedAt(), reason));
                }
            }
        }
        entityManager.flush();
    }

    @Override
    public void recoverActive(Instant now) {
        requireTransaction();
        var ids = entityManager.createQuery("""
                select distinct e.scanId from TestExecutionJpaEntity e where e.status in (:pending, :running)
                """, UUID.class).setParameter("pending", ExecutionStatus.PENDING)
                .setParameter("running", ExecutionStatus.RUNNING).getResultList();
        for (UUID id : ids) {
            closeActive(id, now, ExecutionError.EXECUTION_INTERRUPTED);
        }
    }

    private static void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw ScanOperationException.internal();
        }
    }
}
