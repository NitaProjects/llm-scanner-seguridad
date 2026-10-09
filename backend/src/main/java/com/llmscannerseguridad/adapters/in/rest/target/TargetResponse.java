package com.llmscannerseguridad.adapters.in.rest.target;

import java.util.Map;
import java.util.UUID;
import com.llmscannerseguridad.application.target.TargetView;
import com.llmscannerseguridad.domain.target.AuthType;
import com.llmscannerseguridad.domain.target.TargetSettings;

public record TargetResponse(UUID id, UUID projectId, String name, String url, String method, String contentType,
        String requestTemplate, String promptPlaceholder, String responsePath, Map<String, String> headers,
        String authType, Map<String, String> authConfig, boolean hasCredentials, int timeoutMs,
        String createdAt, String updatedAt) {
    public static TargetResponse from(TargetView view) {
        var settings = view.settings();
        return new TargetResponse(view.id(), view.projectId(), settings.name(), settings.url().value(),
                TargetSettings.METHOD, TargetSettings.CONTENT_TYPE, settings.requestTemplate(),
                TargetSettings.PROMPT_PLACEHOLDER, settings.responsePath(), settings.headers(),
                settings.authType().name(), settings.authType() == AuthType.API_KEY_HEADER
                    ? Map.of("headerName", settings.authHeaderName()) : null,
                view.hasCredentials(), settings.timeoutMs(), view.createdAt().toString(), view.updatedAt().toString());
    }

    @Override
    public String toString() {
        return "TargetResponse[id=" + id + "]";
    }
}
