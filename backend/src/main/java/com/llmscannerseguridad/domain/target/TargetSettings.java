package com.llmscannerseguridad.domain.target;

import java.util.Map;
import java.util.Objects;

public record TargetSettings(String name, TargetUrl url, String requestTemplate, String responsePath,
        Map<String, String> headers, AuthType authType, String authHeaderName, int timeoutMs) {
    public static final String METHOD = "POST";
    public static final String CONTENT_TYPE = "application/json";
    public static final String PROMPT_PLACEHOLDER = "{{PROMPT}}";

    public TargetSettings {
        TargetRules.noNullCharacter(name, "name");
        name = TargetRules.trim(name);
        if (name != null) {
            TargetRules.length(name, 120, "name");
            if (name.isEmpty()) {
                name = null;
            }
        }
        Objects.requireNonNull(url);
        Objects.requireNonNull(authType);
        TargetRules.noNullCharacter(requestTemplate, "requestTemplate");
        if (requestTemplate == null || requestTemplate.isBlank()) {
            throw TargetValidationException.field("requestTemplate", "REQUIRED");
        }
        if (TargetRules.bytes(requestTemplate) > 32768) {
            throw TargetValidationException.field("requestTemplate", "TOO_LONG");
        }
        TargetRules.noNullCharacter(responsePath, "responsePath");
        responsePath = TargetRules.trim(responsePath);
        if (responsePath == null || responsePath.isEmpty()) {
            throw TargetValidationException.field("responsePath", "REQUIRED");
        }
        TargetRules.length(responsePath, 1024, "responsePath");
        if (timeoutMs < 1000 || timeoutMs > 60000) {
            throw TargetValidationException.field("timeoutMs", "OUT_OF_RANGE");
        }
        if (authType == AuthType.API_KEY_HEADER) {
            TargetRules.headerName(authHeaderName, "authConfig.headerName", true);
        } else if (authHeaderName != null) {
            throw TargetValidationException.field("authConfig.headerName", "NOT_ALLOWED");
        }
        if (headers == null) {
            throw TargetValidationException.field("headers", "INVALID_TYPE");
        }
        TargetRules.headers(headers, authHeaderName);
        headers = Map.copyOf(headers);
    }

    @Override
    public String toString() {
        return "TargetSettings";
    }
}
