package com.llmscannerseguridad.adapters.out.execution.scan;

import java.util.UUID;
import com.llmscannerseguridad.application.scan.ScanWorkPort;

public final class NoOpScanWorkAdapter implements ScanWorkPort {
    @Override
    public void execute(UUID scanId) {
        // Unidad temporal de ciclo de vida; COMPLETED no acredita pruebas de seguridad.
    }
}
