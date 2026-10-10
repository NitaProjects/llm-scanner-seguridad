package com.llmscannerseguridad.application.execution;

import java.time.Clock;
import java.util.UUID;
import java.util.function.LongSupplier;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.application.scan.ScanWorkPort;
import com.llmscannerseguridad.domain.execution.ExecutionError;

public final class ExecuteSecurityTestsUseCase implements ScanWorkPort {
    private final TestExecutionRepositoryPort executions;
    private final TargetInvocationPort invocation;
    private final long budgetMs;
    private final Clock clock;
    private final LongSupplier ticker;

    public ExecuteSecurityTestsUseCase(TestExecutionRepositoryPort executions, TargetInvocationPort invocation,
            long budgetMs, Clock clock, LongSupplier ticker) {
        this.executions = executions;
        this.invocation = invocation;
        this.budgetMs = budgetMs;
        this.clock = clock;
        this.ticker = ticker;
    }

    @Override
    public void execute(UUID scanId) {
        long enteredNanos = ticker.getAsLong();
        var enteredAt = clock.instant();
        var plan = executions.prepare(scanId);
        var budget = new ScanBudget(plan.scanStartedAt(), budgetMs, enteredAt, enteredNanos, ticker);
        boolean failed = false;
        for (int i = 0; i < plan.tests().size(); i++) {
            var running = executions.start(plan.executions().get(i).id());
            InvocationResult result;
            try {
                result = budget.remainingNanos() == 0
                        ? InvocationResult.failure(ExecutionError.SCAN_BUDGET_EXCEEDED)
                        : invocation.invoke(plan.target(), plan.tests().get(i).prompt(), budget);
            } catch (RuntimeException error) {
                result = InvocationResult.failure(ExecutionError.EXECUTION_INTERNAL_ERROR);
            }
            // Una escritura fallida aborta inmediatamente; nunca vuelve a invocar.
            executions.finish(running, result);
            if (result.errorReason() != null) {
                failed = true;
                if (result.errorReason().global()) {
                    throw ScanOperationException.internal();
                }
            }
        }
        if (failed || !executions.allCompleted(scanId)) {
            throw ScanOperationException.internal();
        }
    }
}
