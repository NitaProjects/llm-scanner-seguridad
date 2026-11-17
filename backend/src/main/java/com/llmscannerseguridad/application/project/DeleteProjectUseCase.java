package com.llmscannerseguridad.application.project;

import java.util.UUID;

public final class DeleteProjectUseCase {
    private final ProjectDeletionPort deletion;

    public DeleteProjectUseCase(ProjectDeletionPort deletion) {
        this.deletion = deletion;
    }

    public void execute(UUID projectId) {
        deletion.delete(projectId);
    }
}
