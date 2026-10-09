package com.llmscannerseguridad.application.project;

import com.llmscannerseguridad.domain.project.Project;

public interface CreateProjectUseCase {

    Project execute(CreateProjectCommand command);
}
