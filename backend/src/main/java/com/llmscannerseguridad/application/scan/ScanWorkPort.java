package com.llmscannerseguridad.application.scan;

import java.util.UUID;

public interface ScanWorkPort {
    void execute(UUID scanId);
}
