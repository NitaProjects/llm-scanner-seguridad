package com.llmscannerseguridad.config;

import com.llmscannerseguridad.application.scan.RecoverInterruptedScansUseCase;

public final class ScanStartup {
    private final RecoverInterruptedScansUseCase recovery;
    private final ScanExecutionGate gate;

    public ScanStartup(RecoverInterruptedScansUseCase recovery, ScanExecutionGate gate) {
        this.recovery = recovery;
        this.gate = gate;
    }

    public void initialize() {
        try {
            recovery.execute();
        } catch (RuntimeException error) {
            throw ScanRecoveryFailureDiagnostics.failure();
        }
        gate.recovered();
    }
}
