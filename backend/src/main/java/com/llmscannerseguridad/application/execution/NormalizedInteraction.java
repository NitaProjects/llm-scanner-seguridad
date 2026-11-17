package com.llmscannerseguridad.application.execution;

import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence;

public record NormalizedInteraction(String responseText, Long latencyMs, ExecutionError errorReason,
        boolean requestRedacted, boolean responseRedacted, LegacyCompatibility legacy,
        StructuredInteractionEvidence observations) {
    public NormalizedInteraction(String responseText, Long latencyMs, ExecutionError errorReason,
            boolean requestRedacted, boolean responseRedacted, LegacyCompatibility legacy) {
        this(responseText, latencyMs, errorReason, requestRedacted, responseRedacted, legacy, null);
    }

    public NormalizedInteraction withObservations(StructuredInteractionEvidence evidence) {
        return new NormalizedInteraction(responseText, latencyMs, errorReason, requestRedacted, responseRedacted, legacy, evidence);
    }
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
