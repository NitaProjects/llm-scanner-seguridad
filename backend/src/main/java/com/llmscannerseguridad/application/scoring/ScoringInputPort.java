package com.llmscannerseguridad.application.scoring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.domain.evaluation.EvaluationStatus;
import com.llmscannerseguridad.domain.evaluation.Severity;
import com.llmscannerseguridad.domain.evaluation.Verdict;
import com.llmscannerseguridad.domain.execution.ExecutionStatus;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

public interface ScoringInputPort {
    record Execution(UUID id, UUID scanId, UUID testId, String code, int revision, SecurityCategory category,
            ExecutionStatus status, Instant finishedAt) { }
    record Evaluation(UUID id, UUID scanId, UUID executionId, UUID testId, SecurityCategory category,
            EvaluationStatus status, Verdict verdict, Severity severity, BigDecimal confidence, Instant finishedAt) { }
    record Finding(UUID id, UUID scanId, UUID executionId, UUID testId, UUID evaluationId,
            SecurityCategory category, Verdict verdict, Severity severity, BigDecimal confidence, Instant createdAt) { }
    record Inputs(List<Execution> executions, List<Evaluation> evaluations, List<Finding> findings) {
        public Inputs {
            executions = List.copyOf(executions); evaluations = List.copyOf(evaluations); findings = List.copyOf(findings);
        }
    }
    Inputs read(UUID scanId);
}
