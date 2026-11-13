package com.llmscannerseguridad.adapters.out.http.target;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import com.llmscannerseguridad.application.execution.ScanBudget;
import com.llmscannerseguridad.domain.execution.ExecutionError;

public final class InvocationGate {
    private final Semaphore semaphore = new Semaphore(1, true);
    private final long intervalNanos;
    private long previousFinished;
    private boolean finished;

    public InvocationGate(long intervalMs) {
        if (intervalMs < 1000) {
            throw new IllegalArgumentException("El intervalo mínimo es un segundo.");
        }
        intervalNanos = TimeUnit.MILLISECONDS.toNanos(intervalMs);
    }

    public void acquire(ScanBudget budget) {
        acquire(budget, Long.MAX_VALUE);
    }

    void acquire(ScanBudget budget, long deadline) {
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(remaining(budget, deadline), TimeUnit.NANOSECONDS);
            if (!acquired) {
                throw expired(budget, deadline);
            }
            long wait = finished ? intervalNanos - (System.nanoTime() - previousFinished) : 0;
            if (wait > 0) {
                if (wait >= remaining(budget, deadline)) {
                    throw expired(budget, deadline);
                }
                TimeUnit.NANOSECONDS.sleep(wait);
            }
            if (remaining(budget, deadline) == 0) {
                throw expired(budget, deadline);
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            if (acquired) {
                semaphore.release();
            }
            throw new InvocationFailure(deadline == Long.MAX_VALUE ? ExecutionError.EXECUTION_INTERNAL_ERROR : ExecutionError.EXECUTION_INTERRUPTED);
        } catch (RuntimeException error) {
            if (acquired) {
                semaphore.release();
            }
            throw error;
        }
    }

    private static long remaining(ScanBudget budget, long deadline) {
        return deadline == Long.MAX_VALUE ? budget.remainingNanos()
                : Math.min(budget.remainingNanos(), Math.max(0, deadline - System.nanoTime()));
    }

    private static InvocationFailure expired(ScanBudget budget, long deadline) {
        return new InvocationFailure(deadline != Long.MAX_VALUE && deadline - System.nanoTime() < budget.remainingNanos()
                ? ExecutionError.TARGET_TIMEOUT : ExecutionError.SCAN_BUDGET_EXCEEDED);
    }

    public void release() {
        // Medir desde el cierre garantiza separación también si DNS/TLS retrasan el envío.
        previousFinished = System.nanoTime();
        finished = true;
        semaphore.release();
    }
}
