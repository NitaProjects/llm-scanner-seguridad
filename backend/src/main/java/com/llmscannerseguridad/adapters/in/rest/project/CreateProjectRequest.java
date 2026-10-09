package com.llmscannerseguridad.adapters.in.rest.project;

import com.llmscannerseguridad.application.project.CreateProjectCommand;

public record CreateProjectRequest(String name, String description) {

    public CreateProjectCommand toCommand() {
        return new CreateProjectCommand(name, description);
    }
}
