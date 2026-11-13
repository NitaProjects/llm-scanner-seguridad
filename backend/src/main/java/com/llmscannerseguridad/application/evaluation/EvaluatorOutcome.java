package com.llmscannerseguridad.application.evaluation;

import java.math.BigDecimal;
import com.llmscannerseguridad.domain.evaluation.*;

public record EvaluatorOutcome(LlmAssessment assessment, LlmError errorReason, String resolvedModel,
        Long latencyMs, Long inputTokens, Long outputTokens, BigDecimal estimatedCostUsd, String providerRequestId,
        boolean dispatched) {
    public EvaluatorOutcome(LlmAssessment assessment, LlmError errorReason, String resolvedModel,
            Long latencyMs, Long inputTokens, Long outputTokens, BigDecimal estimatedCostUsd, String providerRequestId) {
        this(assessment, errorReason, resolvedModel, latencyMs, inputTokens, outputTokens, estimatedCostUsd, providerRequestId, true);
    }
    public EvaluatorOutcome {
        if ((assessment == null) == (errorReason == null)) throw new IllegalArgumentException("Outcome no válido.");
    }
    public static EvaluatorOutcome failure(LlmError reason) {
        return new EvaluatorOutcome(null, reason, null, null, null, null, null, null);
    }
    public static EvaluatorOutcome notDispatched(LlmError reason) {
        return new EvaluatorOutcome(null, reason, null, null, null, null, null, null, false);
    }
    @Override public String toString() { return "EvaluatorOutcome[errorReason=" + errorReason + "]"; }
}
