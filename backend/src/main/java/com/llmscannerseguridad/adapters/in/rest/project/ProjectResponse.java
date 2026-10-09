package com.llmscannerseguridad.adapters.in.rest.project;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.llmscannerseguridad.domain.project.Project;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ProjectResponse(UUID id, String name, String description, String createdAt, String updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.id(), project.name(), project.description(),
                project.createdAt().toString(), project.updatedAt().toString());
    }
}
