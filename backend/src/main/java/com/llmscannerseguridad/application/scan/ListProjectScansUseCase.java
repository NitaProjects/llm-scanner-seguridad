package com.llmscannerseguridad.application.scan;

import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.domain.scan.Scan;

public final class ListProjectScansUseCase {
    private final ProjectRepositoryPort projects;
    private final ScanRepositoryPort scans;

    public ListProjectScansUseCase(ProjectRepositoryPort projects, ScanRepositoryPort scans) {
        this.projects = projects;
        this.scans = scans;
    }

    public List<Scan> execute(UUID projectId) {
        if (projects.findById(projectId).isEmpty()) {
            throw new ScanOperationException(ScanOperationException.Reason.PROJECT_NOT_FOUND);
        }
        return scans.findByProjectIdOrdered(projectId);
    }
}
