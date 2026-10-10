package com.llmscannerseguridad.application.reporting;

import java.util.Optional;
import java.util.UUID;
import com.llmscannerseguridad.domain.reporting.SecurityReport;

public interface SecurityReportRepositoryPort {
    void setRequiredVersion(UUID scanId);
    ReportSnapshot lock(UUID scanId);
    Optional<ReportSnapshot> findScoped(UUID projectId, UUID scanId);
    SecurityReport insert(SecurityReport candidate);
    void verifyRecoveryIntegrity();
}
