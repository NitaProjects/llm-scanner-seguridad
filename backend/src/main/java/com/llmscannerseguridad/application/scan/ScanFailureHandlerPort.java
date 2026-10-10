package com.llmscannerseguridad.application.scan;

import java.util.UUID;

public interface ScanFailureHandlerPort {
    // Infraestructura bloquea admisión/reclamaciones hasta reiniciar; nunca recibe causas internas.
    void persistenceFailed(UUID scanId);
}
