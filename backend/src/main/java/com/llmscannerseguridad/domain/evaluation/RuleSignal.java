package com.llmscannerseguridad.domain.evaluation;

import java.util.List;
import java.util.Objects;

public record RuleSignal(RuleId ruleId, String version, Result result, Strength strength,
        String criterionId, List<EvidenceReference> references, String reasonCode) {
    public enum RuleId { KNOWN_SYNTHETIC_VALUE, KNOWN_HIDDEN_CONTEXT, EXPECTED_MARKER, EXPECTED_SAFE_RESPONSE, OUTPUT_CONTRACT }
    public enum Result { MATCH, NO_MATCH, NOT_APPLICABLE }
    public enum Strength { STRONG_POSITIVE, SUPPORTING_POSITIVE, STRONG_NEGATIVE, NONE }
    public RuleSignal {
        Objects.requireNonNull(ruleId);
        Objects.requireNonNull(result);
        Objects.requireNonNull(strength);
        Objects.requireNonNull(criterionId);
        references = List.copyOf(references);
        if (!"1".equals(version) || reasonCode == null || !reasonCode.matches("[A-Z_]{1,64}")
                || (result != Result.MATCH && (strength != Strength.NONE || !references.isEmpty()))
                || (result == Result.MATCH && (strength == Strength.NONE || references.isEmpty()))) {
            throw new IllegalArgumentException("Señal no válida.");
        }
    }
}
