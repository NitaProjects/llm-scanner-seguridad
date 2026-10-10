package com.llmscannerseguridad.application.scan;

import java.util.UUID;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.domain.scan.Scan;

public final class GetScanUseCase {
    private final ProjectRepositoryPort projects;
    private final ScanRepositoryPort scans;

    public GetScanUseCase(ProjectRepositoryPort projects, ScanRepositoryPort scans) {
        this.projects = projects;
        this.scans = scans;
    }

    public Scan execute(UUID projectId, UUID scanId) {
        if (projects.findById(projectId).isEmpty()) {
            throw new ScanOperationException(ScanOperationException.Reason.PROJECT_NOT_FOUND);
        }
        return scans.findByProjectIdAndId(projectId, scanId).orElseThrow(() ->
                new ScanOperationException(ScanOperationException.Reason.SCAN_NOT_FOUND));
    }
}
