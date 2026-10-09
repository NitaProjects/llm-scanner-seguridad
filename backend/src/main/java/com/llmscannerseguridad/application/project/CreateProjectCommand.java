package com.llmscannerseguridad.application.project;

import com.llmscannerseguridad.domain.project.Project;

public record CreateProjectCommand(String name, String description) {

    public CreateProjectCommand {
        Project.validateCreation(name, description);
    }
}
