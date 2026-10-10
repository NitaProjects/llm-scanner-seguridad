package com.llmscannerseguridad.application.execution;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.execution.TestExecution;

public interface TestExecutionRepositoryPort {
    ExecutionPlan prepare(UUID scanId);
    TestExecution start(UUID executionId);
    void finish(TestExecution execution, InvocationResult result);
    List<ExecutionMetadata> listMetadata(UUID scanId);
    List<TestExecution> findByScanId(UUID scanId);
    boolean allCompleted(UUID scanId);
    void closeActive(UUID scanId, Instant now, ExecutionError reason);
    void recoverActive(Instant now);
}
