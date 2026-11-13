package com.llmscannerseguridad.application.evaluation;

import java.util.List;
import com.llmscannerseguridad.domain.evaluation.*;

public record EvaluatorInput(EvaluationCriteria criteria, String catalogPrompt, SyntheticContext trustedSyntheticContext,
        List<RuleSignal> deterministicSignals, String evidenceText, boolean complete, boolean redacted,
        SemanticProjection semanticProjection) {
    public EvaluatorInput(EvaluationCriteria criteria, String catalogPrompt, SyntheticContext trustedSyntheticContext,
            List<RuleSignal> deterministicSignals, String evidenceText, boolean complete, boolean redacted) {
        this(criteria, catalogPrompt, trustedSyntheticContext, deterministicSignals, evidenceText, complete, redacted, null);
    }
    public static EvaluatorInput semantic(EvaluationCriteria criteria, SemanticProjection projection) {
        return new EvaluatorInput(criteria, null, null, List.of(), projection.response(), true, false, projection);
    }
    public EvaluatorInput { deterministicSignals = List.copyOf(deterministicSignals); }
    @Override public String toString() { return "EvaluatorInput[protegido]"; }
}
