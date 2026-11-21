package com.llmscannerseguridad.application.execution;

import java.time.Clock;
import java.util.UUID;
import java.util.function.LongSupplier;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.application.scan.ScanWorkPort;
import com.llmscannerseguridad.domain.execution.ExecutionError;

public final class ExecuteSecurityTestsUseCase implements ScanWorkPort {
    private final TestExecutionRepositoryPort executions;
    private final TargetAdapter adapter;
    private final long budgetMs;
    private final DurableEvidenceProtectionPort protection;
    private final Clock clock;
    private final LongSupplier ticker;
    private final com.llmscannerseguridad.application.evaluation.EvaluateExecutionUseCase evaluations;

    public ExecuteSecurityTestsUseCase(TestExecutionRepositoryPort executions, TargetAdapter adapter,
            long budgetMs, Clock clock, LongSupplier ticker) {
        this(executions, adapter, budgetMs, clock, ticker, null);
    }

    public ExecuteSecurityTestsUseCase(TestExecutionRepositoryPort executions, TargetAdapter adapter,
            long budgetMs, Clock clock, LongSupplier ticker,
            com.llmscannerseguridad.application.evaluation.EvaluateExecutionUseCase evaluations) {
        this(executions, adapter, budgetMs, clock, ticker, evaluations,
                new com.llmscannerseguridad.privacy.DurableEvidenceProtection());
    }

    public ExecuteSecurityTestsUseCase(TestExecutionRepositoryPort executions, TargetAdapter adapter,
            long budgetMs, Clock clock, LongSupplier ticker,
            com.llmscannerseguridad.application.evaluation.EvaluateExecutionUseCase evaluations,
            DurableEvidenceProtectionPort protection) {
        this.protection = java.util.Objects.requireNonNull(protection);
        this.executions = executions;
        this.adapter = adapter;
        this.budgetMs = budgetMs;
        this.clock = clock;
        this.ticker = ticker;
        this.evaluations = evaluations;
    }

    @Override
    public void execute(UUID scanId) {
        if (com.llmscannerseguridad.privacy.ProtectionScope.active()) {
            com.llmscannerseguridad.privacy.ProtectionScope.requireScan(scanId);
            executeScoped(scanId);
        } else try (var privacy = com.llmscannerseguridad.privacy.ProtectionScope.open(scanId, 524288)) {
            executeScoped(scanId);
        }
    }

    private void executeScoped(UUID scanId) {
        long enteredNanos = ticker.getAsLong();
        var enteredAt = clock.instant();
        var plan = executions.prepare(scanId);
        var selectedAdapter = plan.integration() == null ? adapter : plan.integration();
        var budget = new ScanBudget(plan.scanStartedAt(), budgetMs, enteredAt, enteredNanos, ticker);
        com.llmscannerseguridad.privacy.ProtectionScope.deadline(() -> budget.remainingNanos() > 0);
        boolean failed = false;
        for (int i = 0; i < plan.tests().size(); i++) {
            var running = executions.start(plan.executions().get(i).id());
            InvocationResult result;
            com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence observations = null;
            try {
                if (!com.llmscannerseguridad.privacy.ProtectionScope.complete()) {
                    result = InvocationResult.failure(ExecutionError.EVIDENCE_LIMIT_EXCEEDED);
                } else if (budget.remainingNanos() == 0) {
                    result = InvocationResult.failure(ExecutionError.SCAN_BUDGET_EXCEEDED);
                } else {
                    var interaction = selectedAdapter.interact(plan.target(), plan.tests().get(i).prompt(),
                            plan.integration() == null ? budget : budget.forExecution(running.id(), plan.tests().get(i).code()));
                    result = LegacyInvocationResultProjection.toLegacy(interaction);
                    if (result.errorReason() == null) observations = interaction.observations();
                }
            } catch (RuntimeException error) {
                result = InvocationResult.failure(ExecutionError.EXECUTION_INTERNAL_ERROR);
            }
            if (observations != null) {
                if (!com.llmscannerseguridad.privacy.ProtectionScope.observationsCovered(running.id())) {
                    observations.localValues().forEach(com.llmscannerseguridad.privacy.ProtectionScope::register);
                }
                if (!running.id().equals(observations.executionId()) || observations.inspectionIncomplete()
                        && !com.llmscannerseguridad.privacy.ProtectionScope.observationsCovered(running.id())) {
                    com.llmscannerseguridad.privacy.ProtectionScope.incomplete();
                }
            }
            result = protection.protect(result, budget);
            // Una escritura fallida aborta inmediatamente; nunca vuelve a invocar.
            executions.finish(running, result);
            if (evaluations != null) {
                var confirmed = executions.findByScanId(scanId).stream().filter(row -> row.id().equals(running.id()))
                        .findFirst().orElseThrow(ScanOperationException::internal);
                if (observations == null) evaluations.execute(confirmed, plan.tests().get(i), budget);
                else evaluations.execute(confirmed, plan.tests().get(i), budget, observations);
            }
            if (result.errorReason() != null) {
                failed = true;
                if (result.errorReason().global()) {
                    throw ScanOperationException.internal();
                }
            }
        }
        if (failed || !executions.allCompleted(scanId) || evaluations != null && !evaluations.allCompleted(scanId)
                || budget.remainingNanos() == 0 || Thread.currentThread().isInterrupted()) {
            throw ScanOperationException.internal();
        }
    }
}
