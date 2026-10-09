package com.llmscannerseguridad.application.project;

import java.util.Objects;
import java.util.UUID;

import com.llmscannerseguridad.domain.project.Project;

public final class GetProjectUseCase {

    private final ProjectRepositoryPort repository;

    public GetProjectUseCase(ProjectRepositoryPort repository) {
        this.repository = repository;
    }

    public Project execute(UUID id) {
        return repository.findById(Objects.requireNonNull(id)).orElseThrow(ProjectNotFoundException::new);
    }
}
