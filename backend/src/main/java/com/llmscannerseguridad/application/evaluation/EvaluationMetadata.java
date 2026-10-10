package com.llmscannerseguridad.application.evaluation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

public record EvaluationMetadata(UUID id, UUID scanId, UUID testExecutionId, String securityTestCode,
        int securityTestRevision, SecurityCategory category, EvaluationStatus status, Verdict finalVerdict,
        BigDecimal confidence, Severity severity, LlmStatus llmStatus, LlmError llmErrorReason,
        String evaluatorProvider, String evaluatorModel, String criteriaId, String criteriaVersion,
        String decisionCode, boolean degraded, boolean evidenceComplete, OutcomeReason outcomeReason,
        EvaluationError errorReason, String reasoningSummary, String recommendation, Instant startedAt,
        Instant finishedAt, UUID findingId) { }
