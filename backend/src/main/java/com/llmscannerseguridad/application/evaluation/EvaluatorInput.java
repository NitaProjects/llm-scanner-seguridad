package com.llmscannerseguridad.application.evaluation;

import java.util.List;
import com.llmscannerseguridad.domain.evaluation.*;

public record EvaluatorInput(EvaluationCriteria criteria, String catalogPrompt, SyntheticContext trustedSyntheticContext,
        List<RuleSignal> deterministicSignals, String evidenceText, boolean complete, boolean redacted,
        SemanticProjection semanticProjection, List<StructuredInteractionEvidence.Observation> additionalSources) {
    public EvaluatorInput(EvaluationCriteria criteria, String catalogPrompt, SyntheticContext trustedSyntheticContext,
            List<RuleSignal> deterministicSignals, String evidenceText, boolean complete, boolean redacted,
            SemanticProjection semanticProjection) {
        this(criteria, catalogPrompt, trustedSyntheticContext, deterministicSignals, evidenceText, complete, redacted,
                semanticProjection, List.of());
    }
    public EvaluatorInput(EvaluationCriteria criteria, String catalogPrompt, SyntheticContext trustedSyntheticContext,
            List<RuleSignal> deterministicSignals, String evidenceText, boolean complete, boolean redacted) {
        this(criteria, catalogPrompt, trustedSyntheticContext, deterministicSignals, evidenceText, complete, redacted, null);
    }
    public static EvaluatorInput semantic(EvaluationCriteria criteria, SemanticProjection projection) {
        return new EvaluatorInput(criteria, null, null, List.of(), projection.response(), true, false, projection);
    }
    public EvaluatorInput {
        deterministicSignals = List.copyOf(deterministicSignals);
        additionalSources = List.copyOf(additionalSources);
    }
    public EvaluatorInput withAdditionalSources(List<StructuredInteractionEvidence.Observation> sources) {
        return new EvaluatorInput(criteria, catalogPrompt, trustedSyntheticContext, deterministicSignals, evidenceText,
                complete, redacted, semanticProjection, sources);
    }
    @Override public String toString() { return "EvaluatorInput[protegido]"; }
}
