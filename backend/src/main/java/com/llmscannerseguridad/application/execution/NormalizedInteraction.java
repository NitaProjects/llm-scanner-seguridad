package com.llmscannerseguridad.application.execution;

import com.llmscannerseguridad.domain.execution.ExecutionError;

public record NormalizedInteraction(String responseText, Long latencyMs, ExecutionError errorReason,
        boolean requestRedacted, boolean responseRedacted, LegacyCompatibility legacy) {
    public record LegacyCompatibility(Integer httpStatus, String requestBody) {
        @Override
        public String toString() {
            return "LegacyCompatibility";
        }
    }

    @Override
    public String toString() {
        return "NormalizedInteraction[errorReason=" + errorReason + "]";
    }
}
