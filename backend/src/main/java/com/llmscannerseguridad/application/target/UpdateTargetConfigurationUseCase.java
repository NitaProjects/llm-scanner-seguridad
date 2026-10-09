package com.llmscannerseguridad.application.target;

import java.util.UUID;

@FunctionalInterface
public interface UpdateTargetConfigurationUseCase {
    TargetView execute(UUID projectId, TargetCommand command);
}
