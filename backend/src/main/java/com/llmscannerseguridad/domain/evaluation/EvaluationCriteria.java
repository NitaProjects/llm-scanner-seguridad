package com.llmscannerseguridad.domain.evaluation;

import java.util.List;
import java.util.Objects;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

public record EvaluationCriteria(String criteriaId, String criteriaVersion, String securityTestCode,
        int securityTestRevision, SecurityCategory category, String objective, String expectedSafeBehavior,
        String violationDefinition, List<String> requiredContextKeys, List<RuleSignal.RuleId> ruleConfigurations,
        String semanticPredicate, String severityPolicyId, String explanationTemplateId,
        String recommendationTemplateId, Severity severityBase, HighImpactCondition highImpactCondition) {
    public enum HighImpactCondition { SECURITY_BOUNDARY_VIOLATION, PROTECTED_REFERENCE_EXPOSED, COMPLETE_HIDDEN_CONTEXT_EXPOSED, OUTPUT_SECURITY_CONTRACT_VIOLATED }
    public EvaluationCriteria {
        Objects.requireNonNull(category);
        Objects.requireNonNull(severityBase);
        requiredContextKeys = List.copyOf(requiredContextKeys);
        ruleConfigurations = List.copyOf(ruleConfigurations);
        if (criteriaId == null || !criteriaId.matches("[A-Z0-9-]{1,64}") || !"1".equals(criteriaVersion)
                || securityTestRevision != 1 || !securityTestCode.startsWith(category.name() + "-")
                || severityBase == Severity.CRITICAL || severityBase == Severity.HIGH
                || objective == null || expectedSafeBehavior == null || violationDefinition == null
                || !"severity-v1".equals(severityPolicyId) || ruleConfigurations.isEmpty()) {
            throw new IllegalArgumentException("Criterio no válido.");
        }
    }
}
