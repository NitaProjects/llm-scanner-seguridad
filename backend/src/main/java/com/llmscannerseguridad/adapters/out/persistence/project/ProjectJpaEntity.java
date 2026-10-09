package com.llmscannerseguridad.adapters.out.persistence.project;

import java.time.Instant;
import java.util.UUID;

import com.llmscannerseguridad.domain.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class ProjectJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 120, updatable = false)
    private String name;

    @Column(length = 2000, updatable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamp(6) with time zone")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false,
            columnDefinition = "timestamp(6) with time zone")
    private Instant updatedAt;

    protected ProjectJpaEntity() {
    }

    public ProjectJpaEntity(Project project) {
        id = project.id();
        name = project.name();
        description = project.description();
        createdAt = project.createdAt();
        updatedAt = project.updatedAt();
    }

    public Project toDomain() {
        return new Project(id, name, description, createdAt, updatedAt);
    }
}
