package com.llmscannerseguridad.application.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.llmscannerseguridad.domain.project.Project;

public interface ProjectRepositoryPort {

    void insert(Project project);

    Optional<Project> findById(UUID id);

    // Orden por createdAt descendente y UUID canónico ascendente.
    List<Project> findAllOrdered();
}
