package com.llmscannerseguridad.application.target;

import java.time.Instant;
import java.util.UUID;
import com.llmscannerseguridad.domain.target.TargetConfiguration;
import com.llmscannerseguridad.domain.target.TargetSettings;

public record TargetView(UUID id, UUID projectId, TargetSettings settings, boolean hasCredentials,
        Instant createdAt, Instant updatedAt) {
    public static TargetView from(TargetConfiguration target) {
        return new TargetView(target.id(), target.projectId(), target.settings(), target.credential() != null,
                target.createdAt(), target.updatedAt());
    }

    @Override
    public String toString() {
        return "TargetView[id=" + id + "]";
    }
}
