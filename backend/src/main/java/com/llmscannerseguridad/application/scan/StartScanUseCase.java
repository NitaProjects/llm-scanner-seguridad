package com.llmscannerseguridad.application.scan;

import java.util.UUID;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanFailureReason;
import com.llmscannerseguridad.domain.scan.ScanStatus;

public final class StartScanUseCase {
    private final ScanLifecyclePort lifecycle;
    private final ScanDispatcherPort dispatcher;
    private final ScanFailureHandlerPort failures;

    public StartScanUseCase(ScanLifecyclePort lifecycle, ScanDispatcherPort dispatcher, ScanFailureHandlerPort failures) {
        this.lifecycle = lifecycle;
        this.dispatcher = dispatcher;
        this.failures = failures;
    }

    public Scan execute(UUID projectId) {
        var initial = lifecycle.create(projectId);
        try {
            dispatcher.dispatch(initial.id());
        } catch (RuntimeException rejected) {
            try {
                if (!lifecycle.fail(initial.id(), ScanStatus.PENDING, ScanFailureReason.SCAN_DISPATCH_FAILED)) {
                    throw ScanOperationException.internal();
                }
            } catch (RuntimeException persistenceFailure) {
                failures.persistenceFailed(initial.id());
                throw new ScanOperationException(ScanOperationException.Reason.INTERNAL_ERROR, projectId, initial.id());
            }
            throw new ScanOperationException(ScanOperationException.Reason.SCAN_DISPATCH_FAILED, projectId, initial.id());
        }
        return initial;
    }
}
