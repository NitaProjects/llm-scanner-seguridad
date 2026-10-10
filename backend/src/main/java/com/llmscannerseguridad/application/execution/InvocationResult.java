package com.llmscannerseguridad.application.execution;

import com.llmscannerseguridad.domain.execution.ExecutionError;

public record InvocationResult(Integer httpStatus, Long latencyMs, ExecutionError errorReason,
        String requestBody, String responseText, boolean requestRedacted, boolean responseRedacted) {
    public static InvocationResult failure(ExecutionError reason) {
        return new InvocationResult(null, null, reason, null, null, false, false);
    }

    @Override
    public String toString() {
        return "InvocationResult[errorReason=" + errorReason + "]";
    }
}
