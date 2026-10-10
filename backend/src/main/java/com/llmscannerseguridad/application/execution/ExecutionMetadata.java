package com.llmscannerseguridad.application.execution;

import java.time.Instant;
import java.util.UUID;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.execution.ExecutionStatus;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

public record ExecutionMetadata(UUID id, UUID scanId, String securityTestCode, int securityTestRevision,
        String securityTestName, SecurityCategory category, ExecutionStatus status, Instant startedAt,
        Instant finishedAt, Integer httpStatus, Long latencyMs, ExecutionError errorReason,
        boolean requestRedacted, boolean responseRedacted) {
}
