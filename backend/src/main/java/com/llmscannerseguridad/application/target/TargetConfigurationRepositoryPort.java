package com.llmscannerseguridad.application.target;

import java.util.Optional;
import java.util.UUID;
import com.llmscannerseguridad.domain.target.TargetConfiguration;

public interface TargetConfigurationRepositoryPort {
    Optional<TargetConfiguration> findByProjectId(UUID projectId);
    void insert(TargetConfiguration target);
    void replace(TargetConfiguration target);
}
