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
        this.delegate = delegate;
        this.gate = gate;
        this.executions = executions;
        this.evaluations = evaluations;
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
            if (evaluations != null) evaluations.recoverActive(Clock.systemUTC().instant());
            if (executions != null) {
                executions.recoverActive(Clock.systemUTC().instant());
            }
            return delegate.recoverInterrupted();
        });
    }
}
