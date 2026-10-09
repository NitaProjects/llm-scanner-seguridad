package com.llmscannerseguridad.application.target;

import java.util.UUID;

@FunctionalInterface
public interface ConfigureTargetUseCase {
    TargetView execute(UUID projectId, TargetCommand command);
}
