package com.llmscannerseguridad.domain.target;

import java.util.Map;
import java.util.Objects;

public record TargetSettings(String name, TargetUrl url, String requestTemplate, String responsePath,
        Map<String, String> headers, AuthType authType, String authHeaderName, int timeoutMs,
        TargetAdapterType adapterType, String model, String systemPrompt) {
    public static final String METHOD = "POST";
    public static final String CONTENT_TYPE = "application/json";
    public static final String PROMPT_PLACEHOLDER = "{{PROMPT}}";

    public TargetSettings(String name, TargetUrl url, String requestTemplate, String responsePath,
            Map<String, String> headers, AuthType authType, String authHeaderName, int timeoutMs) {
        this(name, url, requestTemplate, responsePath, headers, authType, authHeaderName, timeoutMs,
                TargetAdapterType.REST_LEGACY, null, null);
    }

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
        Objects.requireNonNull(adapterType);
        if (adapterType == TargetAdapterType.REST_LEGACY) {
            if (model != null || systemPrompt != null) throw TargetValidationException.field("model", "NOT_ALLOWED");
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
        } else {
            if (requestTemplate != null || responsePath != null) throw TargetValidationException.field("adapterType", "NOT_ALLOWED");
            if (authType != AuthType.BEARER_TOKEN) throw TargetValidationException.field("authType", "NOT_ALLOWED");
            if (model == null || model.isBlank()) throw TargetValidationException.field("model", "REQUIRED");
            model = TargetRules.trim(model);
            if (model.isEmpty()) throw TargetValidationException.field("model", "REQUIRED");
            TargetRules.length(model, 200, "model");
            if (model.codePoints().anyMatch(Character::isISOControl) || model.contains(PROMPT_PLACEHOLDER)
                    || com.llmscannerseguridad.domain.securitytest.SecurityTest.invalidUnicode(model)) {
                throw TargetValidationException.field("model", "INVALID_FORMAT");
            }
            if (systemPrompt != null) {
                TargetRules.noNullCharacter(systemPrompt, "systemPrompt");
                if (TargetRules.trim(systemPrompt).isEmpty() || TargetRules.bytes(systemPrompt) > 16384 || systemPrompt.contains(PROMPT_PLACEHOLDER)
                        || com.llmscannerseguridad.domain.securitytest.SecurityTest.invalidUnicode(systemPrompt)) {
                    throw TargetValidationException.field("systemPrompt", "INVALID_FORMAT");
                }
            }
            if (headers == null || !headers.isEmpty()) throw TargetValidationException.field("headers", "NOT_ALLOWED");
        }
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
