package com.llmscannerseguridad.domain.target;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TargetConfiguration(UUID id, UUID projectId, TargetSettings settings,
        EncryptedCredential credential, Instant createdAt, Instant updatedAt) {
    public TargetConfiguration {
        Objects.requireNonNull(id);
        Objects.requireNonNull(projectId);
        Objects.requireNonNull(settings);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        if (updatedAt.isBefore(createdAt) || createdAt.getNano() % 1000 != 0 || updatedAt.getNano() % 1000 != 0
                || ((settings.authType() == AuthType.NONE) != (credential == null))) {
            throw new IllegalArgumentException("Estado de configuración no válido.");
        }
    }

    @Override
    public String toString() {
        return "TargetConfiguration[id=" + id + ", projectId=" + projectId + "]";
    }
}
