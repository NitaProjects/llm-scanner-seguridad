package com.llmscannerseguridad.domain.finding;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

public record Finding(UUID id, UUID evaluationResultId, UUID testExecutionId, UUID scanId, UUID securityTestId,
        SecurityCategory category, Severity severity, BigDecimal confidence, String title, String description,
        String evidenceSummary, String recommendation, Instant createdAt) {
    public Finding {
        Objects.requireNonNull(id); Objects.requireNonNull(evaluationResultId); Objects.requireNonNull(testExecutionId);
        Objects.requireNonNull(scanId); Objects.requireNonNull(securityTestId); Objects.requireNonNull(category);
        Objects.requireNonNull(severity); Objects.requireNonNull(confidence); Objects.requireNonNull(createdAt);
        if (confidence.signum() < 0 || confidence.compareTo(BigDecimal.ONE) > 0 || !safe(title, 150)
                || !safe(description, 1000) || !safe(evidenceSummary, 1000) || !safe(recommendation, 1000)) {
            throw new IllegalArgumentException("Finding no válido.");
        }
    }
    public Verdict verdict() { return Verdict.VULNERABLE; }
    public static Finding from(EvaluationResult result) {
        if (result.status() != EvaluationStatus.COMPLETED || result.decision().finalVerdict() != Verdict.VULNERABLE) {
            throw new IllegalArgumentException("Solo un resultado vulnerable puede crear un Finding.");
        }
        return new Finding(UUID.randomUUID(), result.id(), result.testExecutionId(), result.scanId(), result.securityTestId(),
                result.category(), result.decision().severity(), result.decision().confidence(),
                "Incumplimiento controlado de " + result.category().name(), result.reasoningSummary(),
                result.evidenceSummary(), result.recommendation(), result.finishedAt());
    }
    private static boolean safe(String text, int max) {
        return text != null && !text.isBlank() && text.codePointCount(0, text.length()) <= max
                && text.codePoints().noneMatch(c -> c == '<' || c == '>' || c == '`' || c == 0);
    }
}
