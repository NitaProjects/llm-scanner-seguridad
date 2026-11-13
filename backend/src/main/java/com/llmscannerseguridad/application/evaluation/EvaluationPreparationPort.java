package com.llmscannerseguridad.application.evaluation;

import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.execution.TestExecution;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;

public interface EvaluationPreparationPort {
    final class Failure extends RuntimeException {
        private final EvaluationError reason;
        public Failure(EvaluationError reason) { super("No se pudo preparar la evaluación."); this.reason = reason; }
        public EvaluationError reason() { return reason; }
    }
    record Prepared(EvaluationResult result, EvaluationCriteria criteria, SyntheticContext context,
            EvaluatorInput input, OutcomeReason insufficiency) {
        public boolean requestLlm() { return input != null; }
    }
    Prepared prepare(EvaluationResult pending, TestExecution execution, SecurityTest test);
}
