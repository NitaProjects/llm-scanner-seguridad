package com.llmscannerseguridad.config;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.time.Clock;
import com.llmscannerseguridad.application.execution.TestExecutionRepositoryPort;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.adapters.out.persistence.scan.ScanPersistenceErrors;
import com.llmscannerseguridad.application.scan.ScanLifecyclePort;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanFailureReason;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalScanLifecycle implements ScanLifecyclePort {
    private final ScanLifecyclePort delegate;
    private final ScanExecutionGate gate;
    private final TransactionTemplate transaction;
    private final TestExecutionRepositoryPort executions;
    private final com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort evaluations;
    private final com.llmscannerseguridad.application.scoring.ScanScoreRepositoryPort scores;
    private final com.llmscannerseguridad.application.scoring.CalculateScanScoreUseCase calculateScore;
    private final com.llmscannerseguridad.application.scan.ScanRepositoryPort scans;
    private final com.llmscannerseguridad.application.reporting.SecurityReportRepositoryPort reports;
    private final com.llmscannerseguridad.application.reporting.GenerateReportUseCase generateReport;

    public TransactionalScanLifecycle(ScanLifecyclePort delegate, PlatformTransactionManager manager,
            ScanExecutionGate gate) {
        this(delegate, manager, gate, null);
    }

    public TransactionalScanLifecycle(ScanLifecyclePort delegate, PlatformTransactionManager manager,
            ScanExecutionGate gate, TestExecutionRepositoryPort executions) {
        this(delegate, manager, gate, executions, null);
    }

    public TransactionalScanLifecycle(ScanLifecyclePort delegate, PlatformTransactionManager manager,
            ScanExecutionGate gate, TestExecutionRepositoryPort executions,
            com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort evaluations) {
        this(delegate, manager, gate, executions, evaluations, null, null, null);
    }

    public TransactionalScanLifecycle(ScanLifecyclePort delegate, PlatformTransactionManager manager,
            ScanExecutionGate gate, TestExecutionRepositoryPort executions,
            com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort evaluations,
            com.llmscannerseguridad.application.scoring.ScanScoreRepositoryPort scores,
            com.llmscannerseguridad.application.scoring.CalculateScanScoreUseCase calculateScore,
            com.llmscannerseguridad.application.scan.ScanRepositoryPort scans) {
        this(delegate,manager,gate,executions,evaluations,scores,calculateScore,scans,null,null);
    }

    public TransactionalScanLifecycle(ScanLifecyclePort delegate, PlatformTransactionManager manager,
            ScanExecutionGate gate, TestExecutionRepositoryPort executions,
            com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort evaluations,
            com.llmscannerseguridad.application.scoring.ScanScoreRepositoryPort scores,
            com.llmscannerseguridad.application.scoring.CalculateScanScoreUseCase calculateScore,
            com.llmscannerseguridad.application.scan.ScanRepositoryPort scans,
            com.llmscannerseguridad.application.reporting.SecurityReportRepositoryPort reports,
            com.llmscannerseguridad.application.reporting.GenerateReportUseCase generateReport) {
        this.delegate = delegate;
        this.gate = gate;
        this.executions = executions;
        this.evaluations = evaluations;
        this.scores = scores;
        this.calculateScore = calculateScore;
        this.scans = scans;
        this.reports = reports;
        this.generateReport = generateReport;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private <T> T confirmed(Supplier<T> operation) {
        try {
            // Incluye flush y commit; no se devuelve un resultado provisional.
            return transaction.execute(status -> operation.get());
        } catch (RuntimeException error) {
            throw ScanPersistenceErrors.translate(error);
        }
    }

    @Override
    public Scan create(UUID projectId) {
        return gate.admit(() -> confirmed(() -> {
            var scan = delegate.create(projectId);
            if (evaluations != null) evaluations.setProfile(scan.id());
            if (scores != null) scores.setRequiredVersion(scan.id());
            if (reports != null) reports.setRequiredVersion(scan.id());
            return scan;
        }));
    }

    @Override
    public Optional<Scan> claim(UUID scanId) {
        return gate.admit(() -> confirmed(() -> delegate.claim(scanId)));
    }

    @Override
    public boolean complete(UUID scanId) {
        return confirmed(() -> {
            var reportSnapshot = reports == null ? null : reports.lock(scanId);
            if (reportSnapshot != null) reportSnapshot.validate();
            if (scores != null) {
                var snapshot = scores.lock(scanId);
                snapshot.validate();
                if (!snapshot.scan().status().active()) return snapshot.scan().status() == ScanStatus.COMPLETED;
                if (snapshot.requiredVersion() != null) {
                    var score = calculateScore.execute(snapshot);
                    scores.insert(score);
                    var floor = score.calculatedAt();
                    if (reportSnapshot != null && reportSnapshot.reportVersion() != null) {
                        var report = reports.insert(generateReport.execute(scanId));
                        floor = report.generatedAt();
                    }
                    var now = Clock.systemUTC().instant();
                    var finished = now.isBefore(floor) ? floor : now;
                    if (!scans.transition(ScanStatus.RUNNING, snapshot.scan().complete(finished))) throw ScanOperationException.internal();
                    return true;
                }
            }
            if (executions != null && !executions.allCompleted(scanId)) {
                throw ScanOperationException.internal();
            }
            if (evaluations != null && !evaluations.allCompleted(scanId)) throw ScanOperationException.internal();
            return delegate.complete(scanId);
        });
    }

    @Override
    public boolean fail(UUID scanId, ScanStatus expected, ScanFailureReason reason) {
        return confirmed(() -> {
            if (reports != null) reports.lock(scanId).validate();
            if (scores != null) scores.lock(scanId).validate();
            boolean changed = delegate.fail(scanId, expected, reason);
            if (changed && evaluations != null) {
                evaluations.closeActive(scanId, Clock.systemUTC().instant(),
                        com.llmscannerseguridad.domain.evaluation.EvaluationError.EVALUATION_ABORTED);
            }
            if (changed && executions != null) {
                executions.closeActive(scanId, Clock.systemUTC().instant(), ExecutionError.EXECUTION_ABORTED);
            }
            return changed;
        });
    }

    @Override
    public int recoverInterrupted() {
        return confirmed(() -> {
            if (reports != null) reports.verifyRecoveryIntegrity();
            if (scores != null) scores.verifyRecoveryIntegrity();
            if (evaluations != null) evaluations.recoverActive(Clock.systemUTC().instant());
            if (executions != null) {
                executions.recoverActive(Clock.systemUTC().instant());
            }
            return delegate.recoverInterrupted();
        });
    }
}
