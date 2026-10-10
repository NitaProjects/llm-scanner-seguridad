package com.llmscannerseguridad.config;

import java.util.UUID;
import com.llmscannerseguridad.application.scan.ProcessScanUseCase;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class ScanWorkerEntry {
    private final ProcessScanUseCase process;
    private final ScanExecutionGate gate;
    private final TransactionTemplate withoutTransaction;

    public ScanWorkerEntry(ProcessScanUseCase process, PlatformTransactionManager manager, ScanExecutionGate gate) {
        this.process = process;
        this.gate = gate;
        withoutTransaction = new TransactionTemplate(manager);
        withoutTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    }

    public void execute(UUID scanId) {
        try {
            // También protege entradas con una transacción ambiente: solo T2/T3 son transaccionales.
            withoutTransaction.executeWithoutResult(status -> process.execute(scanId));
        } catch (RuntimeException error) {
            gate.persistenceFailed(scanId);
        }
    }
}
