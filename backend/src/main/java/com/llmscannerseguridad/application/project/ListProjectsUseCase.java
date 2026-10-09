package com.llmscannerseguridad.application.project;

import java.util.List;

import com.llmscannerseguridad.domain.project.Project;

public final class ListProjectsUseCase {

    private final ProjectRepositoryPort repository;

    public ListProjectsUseCase(ProjectRepositoryPort repository) {
        this.repository = repository;
    }

    public List<Project> execute() {
        return List.copyOf(repository.findAllOrdered());
    }
}
