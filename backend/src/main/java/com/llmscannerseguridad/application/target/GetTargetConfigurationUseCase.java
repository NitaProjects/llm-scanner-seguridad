package com.llmscannerseguridad.application.target;

import java.util.UUID;
import com.llmscannerseguridad.application.project.ProjectNotFoundException;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;

public final class GetTargetConfigurationUseCase {
    private final ProjectRepositoryPort projects;
    private final TargetConfigurationRepositoryPort targets;

    public GetTargetConfigurationUseCase(ProjectRepositoryPort projects, TargetConfigurationRepositoryPort targets) {
        this.projects = projects;
        this.targets = targets;
    }

    public TargetView execute(UUID projectId) {
        projects.findById(projectId).orElseThrow(ProjectNotFoundException::new);
        return targets.findByProjectId(projectId).map(TargetView::from)
                .orElseThrow(() -> new TargetOperationException(TargetOperationException.Reason.TARGET_NOT_CONFIGURED));
    }
}
