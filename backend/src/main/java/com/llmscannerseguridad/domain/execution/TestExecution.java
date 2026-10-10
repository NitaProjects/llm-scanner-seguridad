package com.llmscannerseguridad.domain.execution;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public record TestExecution(UUID id, UUID scanId, UUID securityTestId, UUID targetConfigurationId,
        Instant targetUpdatedAt, ExecutionStatus status, Instant startedAt, Instant finishedAt,
        Integer httpStatus, Long latencyMs, ExecutionError errorReason, String requestBody,
        String responseText, boolean requestRedacted, boolean responseRedacted) {
    public TestExecution {
        Objects.requireNonNull(id);
        Objects.requireNonNull(scanId);
        Objects.requireNonNull(securityTestId);
        Objects.requireNonNull(targetConfigurationId);
        Objects.requireNonNull(targetUpdatedAt);
        Objects.requireNonNull(status);
        for (var instant : new Instant[] {targetUpdatedAt, startedAt, finishedAt}) {
            if (instant != null && instant.getNano() % 1000 != 0) {
                throw new IllegalArgumentException("Las fechas requieren precisión de microsegundos.");
            }
        }
        boolean empty = httpStatus == null && latencyMs == null && requestBody == null && responseText == null;
        boolean lifecycle = switch (status) {
            case PENDING -> startedAt == null && finishedAt == null && errorReason == null && empty;
            case RUNNING -> startedAt != null && finishedAt == null && errorReason == null && empty;
            case COMPLETED -> startedAt != null && finishedAt != null && errorReason == null
                    && httpStatus != null && httpStatus >= 200 && httpStatus <= 299
                    && latencyMs != null && requestBody != null && responseText != null;
            case FAILED -> finishedAt != null && errorReason != null && (startedAt != null
                    || (empty && (errorReason == ExecutionError.EXECUTION_ABORTED
                        || errorReason == ExecutionError.EXECUTION_INTERRUPTED)));
        };
        if (!lifecycle || (httpStatus != null && (httpStatus < 100 || httpStatus > 599))
                || (latencyMs != null && latencyMs < 0)
                || (startedAt != null && finishedAt != null && finishedAt.isBefore(startedAt))
                || (requestBody == null && requestRedacted) || (responseText == null && responseRedacted)) {
            throw new IllegalArgumentException("El estado técnico de la ejecución no es válido.");
        }
    }

    public static TestExecution pending(UUID id, UUID scanId, UUID testId, UUID targetId, Instant updatedAt) {
        return new TestExecution(id, scanId, testId, targetId, updatedAt, ExecutionStatus.PENDING,
                null, null, null, null, null, null, null, false, false);
    }

    public TestExecution start(Instant now, Instant scanStartedAt) {
        require(ExecutionStatus.PENDING);
        return new TestExecution(id, scanId, securityTestId, targetConfigurationId, targetUpdatedAt,
                ExecutionStatus.RUNNING, latest(now, scanStartedAt), null, null, null, null,
                null, null, false, false);
    }

    public TestExecution finish(Instant now, Integer http, Long latency, ExecutionError error,
            String request, String response, boolean redactedRequest, boolean redactedResponse) {
        require(ExecutionStatus.RUNNING);
        return new TestExecution(id, scanId, securityTestId, targetConfigurationId, targetUpdatedAt,
                error == null ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED,
                startedAt, latest(now, startedAt), http, latency, error, request, response,
                redactedRequest, redactedResponse);
    }

    public TestExecution abort(Instant now, Instant scanStartedAt, ExecutionError reason) {
        if (!status.active() || (reason != ExecutionError.EXECUTION_ABORTED
                && reason != ExecutionError.EXECUTION_INTERRUPTED)) {
            throw new IllegalStateException("La transición de ejecución no está permitida.");
        }
        return new TestExecution(id, scanId, securityTestId, targetConfigurationId, targetUpdatedAt,
                ExecutionStatus.FAILED, startedAt, latest(now, startedAt == null ? scanStartedAt : startedAt),
                null, null, reason, null, null, false, false);
    }

    private void require(ExecutionStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("La transición de ejecución no está permitida.");
        }
    }

    private static Instant latest(Instant now, Instant floor) {
        Instant value = now.truncatedTo(ChronoUnit.MICROS);
        return value.isBefore(floor) ? floor : value;
    }

    @Override
    public String toString() {
        return "TestExecution[id=" + id + ", scanId=" + scanId + ", status=" + status + "]";
    }
}
