package com.llmscannerseguridad.application.project;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import com.llmscannerseguridad.domain.project.Project;

public final class CreateProjectService implements CreateProjectUseCase {

    private final ProjectRepositoryPort repository;
    private final Clock clock;
    private final Supplier<UUID> identifiers;

    public CreateProjectService(ProjectRepositoryPort repository, Clock clock, Supplier<UUID> identifiers) {
        this.repository = repository;
        this.clock = clock;
        this.identifiers = identifiers;
    }

    @Override
    public Project execute(CreateProjectCommand command) {
        Objects.requireNonNull(command);
        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        var project = new Project(identifiers.get(), command.name(), command.description(), now, now);
        repository.insert(project);
        return project;
    }
}
