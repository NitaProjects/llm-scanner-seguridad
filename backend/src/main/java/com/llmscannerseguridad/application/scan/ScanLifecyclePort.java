package com.llmscannerseguridad.application.scan;

import java.util.Optional;
import java.util.UUID;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanFailureReason;
import com.llmscannerseguridad.domain.scan.ScanStatus;

public interface ScanLifecyclePort {
    // La composición confirma cada operación en una transacción independiente antes de retornar.
    Scan create(UUID projectId);
    Optional<Scan> claim(UUID scanId);
    boolean complete(UUID scanId);
    // Compensar solo desde el estado confirmado propio, sin afectar un reclamo ajeno.
    boolean fail(UUID scanId, ScanStatus expected, ScanFailureReason reason);
    int recoverInterrupted();
}
