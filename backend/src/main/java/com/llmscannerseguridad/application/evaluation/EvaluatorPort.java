package com.llmscannerseguridad.application.evaluation;

public interface EvaluatorPort {
    EvaluatorOutcome evaluate(EvaluatorInput input, EvaluationCallBudget budget);
}
