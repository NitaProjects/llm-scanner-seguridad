package com.llmscannerseguridad.config;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
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

    public TransactionalScanLifecycle(ScanLifecyclePort delegate, PlatformTransactionManager manager,
            ScanExecutionGate gate) {
        this.delegate = delegate;
        this.gate = gate;
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
        return gate.admit(() -> confirmed(() -> delegate.create(projectId)));
    }

    @Override
    public Optional<Scan> claim(UUID scanId) {
        return gate.admit(() -> confirmed(() -> delegate.claim(scanId)));
    }

    @Override
    public boolean complete(UUID scanId) {
        return confirmed(() -> delegate.complete(scanId));
    }

    @Override
    public boolean fail(UUID scanId, ScanStatus expected, ScanFailureReason reason) {
        return confirmed(() -> delegate.fail(scanId, expected, reason));
    }

    @Override
    public int recoverInterrupted() {
        return confirmed(delegate::recoverInterrupted);
    }
}
