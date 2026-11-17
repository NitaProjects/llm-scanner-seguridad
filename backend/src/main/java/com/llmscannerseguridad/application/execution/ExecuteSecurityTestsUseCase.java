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
        this.executions = executions;
        this.adapter = adapter;
        this.budgetMs = budgetMs;
        this.clock = clock;
        this.ticker = ticker;
        this.evaluations = evaluations;
    }

    @Override
    public void execute(UUID scanId) {
        long enteredNanos = ticker.getAsLong();
        var enteredAt = clock.instant();
        var plan = executions.prepare(scanId);
        var selectedAdapter = plan.integration() == null ? adapter : plan.integration();
        var budget = new ScanBudget(plan.scanStartedAt(), budgetMs, enteredAt, enteredNanos, ticker);
        boolean failed = false;
        for (int i = 0; i < plan.tests().size(); i++) {
            var running = executions.start(plan.executions().get(i).id());
            InvocationResult result;
            com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence observations = null;
            try {
                if (budget.remainingNanos() == 0) {
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
