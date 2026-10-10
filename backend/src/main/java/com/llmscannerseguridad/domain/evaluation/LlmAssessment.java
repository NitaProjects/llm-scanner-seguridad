package com.llmscannerseguridad.domain.evaluation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;

public record LlmAssessment(Verdict verdict, BigDecimal confidence, String reasoningSummary,
        Severity suggestedSeverity, String recommendation, List<EvidenceReference> evidenceReferences) {
    public LlmAssessment {
        Objects.requireNonNull(verdict);
        if (confidence == null || confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0
                || !text(reasoningSummary) || !text(recommendation)
                || ((verdict == Verdict.VULNERABLE) != (suggestedSeverity != null))) {
            throw new IllegalArgumentException("Assessment no válido.");
        }
        evidenceReferences = List.copyOf(evidenceReferences);
        if (evidenceReferences.size() > 3 || (verdict != Verdict.INCONCLUSIVE && evidenceReferences.isEmpty())) {
            throw new IllegalArgumentException("Referencias no válidas.");
        }
    }
    private static boolean text(String value) {
        return value != null && !value.isBlank() && value.codePointCount(0, value.length()) <= 600
                && !SecurityTest.invalidUnicode(value);
    }
    @Override public String toString() { return "LlmAssessment[verdict=" + verdict + "]"; }
}
