package com.llmscannerseguridad.application.project;

import java.util.UUID;

@FunctionalInterface
public interface ProjectDeletionPort {
    void delete(UUID projectId);
}
