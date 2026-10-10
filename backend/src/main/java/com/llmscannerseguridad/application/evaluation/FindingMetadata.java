package com.llmscannerseguridad.application.evaluation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

public record FindingMetadata(UUID id, UUID evaluationResultId, UUID scanId, UUID testExecutionId,
        String securityTestCode, int securityTestRevision, SecurityCategory category, Verdict verdict,
        Severity severity, BigDecimal confidence, String title, String description, String evidenceSummary,
        String recommendation, Instant createdAt) { }
