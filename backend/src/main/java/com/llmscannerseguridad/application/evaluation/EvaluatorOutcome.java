package com.llmscannerseguridad.application.evaluation;

import java.math.BigDecimal;
import com.llmscannerseguridad.domain.evaluation.*;

public record EvaluatorOutcome(LlmAssessment assessment, LlmError errorReason, String resolvedModel,
        Long latencyMs, Long inputTokens, Long outputTokens, BigDecimal estimatedCostUsd, String providerRequestId) {
    public EvaluatorOutcome {
        if ((assessment == null) == (errorReason == null)) throw new IllegalArgumentException("Outcome no válido.");
    }
    public static EvaluatorOutcome failure(LlmError reason) {
        return new EvaluatorOutcome(null, reason, null, null, null, null, null, null);
    }
    @Override public String toString() { return "EvaluatorOutcome[errorReason=" + errorReason + "]"; }
}
