package com.llmscannerseguridad.application.evaluation;

import java.util.List;
import com.llmscannerseguridad.domain.evaluation.*;

public record EvaluatorInput(EvaluationCriteria criteria, String catalogPrompt, SyntheticContext trustedSyntheticContext,
        List<RuleSignal> deterministicSignals, String evidenceText, boolean complete, boolean redacted) {
    public EvaluatorInput { deterministicSignals = List.copyOf(deterministicSignals); }
    @Override public String toString() { return "EvaluatorInput[protegido]"; }
}
