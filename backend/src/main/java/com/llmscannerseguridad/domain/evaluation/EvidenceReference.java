package com.llmscannerseguridad.domain.evaluation;

import java.util.Objects;

public record EvidenceReference(String evidenceId, int start, int end, String criterionId) {
    public EvidenceReference {
        Objects.requireNonNull(criterionId);
        if (!"response".equals(evidenceId) || start < 0 || end <= start || end - start > 240) {
            throw new IllegalArgumentException("Referencia de evidencia no válida.");
        }
    }

    public boolean validFor(String text, String criterion) {
        return criterionId.equals(criterion) && end <= text.codePointCount(0, text.length());
    }
}
