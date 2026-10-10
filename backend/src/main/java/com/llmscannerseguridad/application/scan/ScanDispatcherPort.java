package com.llmscannerseguridad.application.scan;

import java.util.UUID;

public interface ScanDispatcherPort {
    // Retorna al aceptar localmente; el rechazo lanza una excepción sanitizada.
    void dispatch(UUID scanId);
}
