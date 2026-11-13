package com.llmscannerseguridad.application.evaluation;

import java.util.List;
import com.llmscannerseguridad.domain.evaluation.EvaluationCriteria;
import com.llmscannerseguridad.domain.evaluation.RuleSignal;
import com.llmscannerseguridad.domain.evaluation.SyntheticContext;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

/** Datos admitidos en el mensaje semántico v2. Las referencias se validan localmente. */
public record SemanticProjection(Criterion criterion, String catalogPrompt, Parameters parameters,
        List<Signal> signals, String response) {
    public record Criterion(String criteriaId, SecurityCategory category, String objective,
            String expectedSafeBehavior, String violationDefinition) { }
    public sealed interface Parameters permits Restriction, PartialAnchors, NoParameters { }
    public record Restriction(String restriction) implements Parameters { }
    public record PartialAnchors(List<String> hiddenAnchors) implements Parameters {
        public PartialAnchors { hiddenAnchors = List.copyOf(hiddenAnchors); }
    }
    public record NoParameters() implements Parameters { }
    public record Signal(RuleSignal.RuleId ruleId, RuleSignal.Strength strength) { }

    public SemanticProjection { signals = List.copyOf(signals); }

    public static SemanticProjection from(EvaluationCriteria criteria, String prompt, SyntheticContext context,
            List<RuleSignal> localSignals, String response) {
        var category = criteria.category();
        var signals = localSignals.stream().filter(s -> s.result() == RuleSignal.Result.MATCH)
                .filter(s -> s.ruleId() == RuleSignal.RuleId.EXPECTED_SAFE_RESPONSE && s.strength() == RuleSignal.Strength.STRONG_NEGATIVE
                        || supports(category, s))
                .map(s -> new Signal(s.ruleId(), s.strength())).toList();
        boolean support = signals.stream().anyMatch(s -> s.strength() == RuleSignal.Strength.SUPPORTING_POSITIVE);
        Parameters parameters = switch (category) {
            case LLM01 -> new Restriction(context.restriction());
            case LLM08 -> support ? new PartialAnchors(context.hiddenAnchors()) : new NoParameters();
            case LLM02, LLM10 -> new NoParameters();
        };
        String semanticPrompt = switch (category) {
            case LLM01, LLM10 -> null; // El criterio describe el objetivo sin el marcador público ni el span.
            case LLM02, LLM08 -> prompt;
        };
        return new SemanticProjection(new Criterion(criteria.criteriaId(), category, criteria.objective(),
                criteria.expectedSafeBehavior(), criteria.violationDefinition()), semanticPrompt, parameters, signals, response);
    }

    private static boolean supports(SecurityCategory category, RuleSignal signal) {
        return signal.strength() == RuleSignal.Strength.SUPPORTING_POSITIVE
                && (category == SecurityCategory.LLM01 && signal.ruleId() == RuleSignal.RuleId.EXPECTED_MARKER
                || category == SecurityCategory.LLM08 && signal.ruleId() == RuleSignal.RuleId.KNOWN_HIDDEN_CONTEXT);
    }

    @Override public String toString() { return "SemanticProjection[protegida]"; }
}
