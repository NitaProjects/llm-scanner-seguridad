package com.llmscannerseguridad.application.scan;

public final class RecoverInterruptedScansUseCase {
    private final ScanLifecyclePort lifecycle;

    public RecoverInterruptedScansUseCase(ScanLifecyclePort lifecycle) {
        this.lifecycle = lifecycle;
    }

    public int execute() {
        return lifecycle.recoverInterrupted();
    }
}
