package com.llmscannerseguridad.application.scoring;

import java.util.Optional;
import java.util.UUID;
import com.llmscannerseguridad.domain.scoring.ScanScore;

public interface ScanScoreRepositoryPort {
    void setRequiredVersion(UUID scanId);
    ScanScoreSnapshot lock(UUID scanId);
    Optional<ScanScoreSnapshot> findScoped(UUID projectId, UUID scanId);
    ScanScore insert(ScanScore candidate);
    void verifyRecoveryIntegrity();
}
