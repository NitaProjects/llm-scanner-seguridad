package com.llmscannerseguridad.application.scan;

import java.util.UUID;
import com.llmscannerseguridad.domain.scan.ScanFailureReason;
import com.llmscannerseguridad.domain.scan.ScanStatus;

public final class ProcessScanUseCase {
    private final ScanLifecyclePort lifecycle;
    private final ScanWorkPort work;
    private final ScanFailureHandlerPort failures;

    public ProcessScanUseCase(ScanLifecyclePort lifecycle, ScanWorkPort work, ScanFailureHandlerPort failures) {
        this.lifecycle = lifecycle;
        this.work = work;
        this.failures = failures;
    }

    public void execute(UUID scanId) {
        try (var privacy = com.llmscannerseguridad.privacy.ProtectionScope.open(scanId, 524288)) {
            executeScoped(scanId);
        }
    }

    private void executeScoped(UUID scanId) {
        try {
            if (lifecycle.claim(scanId).isEmpty()) {
                return;
            }
        } catch (ScanOperationException error) {
            if (error.reason() == ScanOperationException.Reason.SCAN_EXECUTION_UNAVAILABLE) {
                return;
            }
            failedClaim(scanId);
            return;
        } catch (RuntimeException error) {
            failedClaim(scanId);
            return;
        }
        try {
            work.execute(scanId);
            if (!lifecycle.complete(scanId)) {
                throw ScanOperationException.internal();
            }
        } catch (RuntimeException error) {
            fail(scanId, ScanStatus.RUNNING);
        }
    }

    private void failedClaim(UUID scanId) {
        try {
            // Un false no acredita rollback: RUNNING puede pertenecer a otra entrega
            // o a un commit cuya confirmación se perdió. No se modifica ese estado.
            if (!lifecycle.fail(scanId, ScanStatus.PENDING, ScanFailureReason.SCAN_PROCESSING_FAILED)) {
                failures.persistenceFailed(scanId);
            }
        } catch (RuntimeException error) {
            failures.persistenceFailed(scanId);
        }
    }

    private void fail(UUID scanId, ScanStatus expected) {
        try {
            // false conserva un recurso ya terminal; no se reabre ni se repite trabajo.
            lifecycle.fail(scanId, expected, ScanFailureReason.SCAN_PROCESSING_FAILED);
        } catch (RuntimeException error) {
            failures.persistenceFailed(scanId);
        }
    }
}
