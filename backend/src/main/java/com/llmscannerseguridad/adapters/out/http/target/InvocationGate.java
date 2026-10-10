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
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(budget.remainingNanos(), TimeUnit.NANOSECONDS);
            if (!acquired) {
                throw new InvocationFailure(ExecutionError.SCAN_BUDGET_EXCEEDED);
            }
            long wait = finished ? intervalNanos - (System.nanoTime() - previousFinished) : 0;
            if (wait > 0) {
                if (wait >= budget.remainingNanos()) {
                    throw new InvocationFailure(ExecutionError.SCAN_BUDGET_EXCEEDED);
                }
                TimeUnit.NANOSECONDS.sleep(wait);
            }
            if (budget.remainingNanos() == 0) {
                throw new InvocationFailure(ExecutionError.SCAN_BUDGET_EXCEEDED);
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            if (acquired) {
                semaphore.release();
            }
            throw new InvocationFailure(ExecutionError.EXECUTION_INTERNAL_ERROR);
        } catch (RuntimeException error) {
            if (acquired) {
                semaphore.release();
            }
            throw error;
        }
    }

    public void release() {
        // Medir desde el cierre garantiza separación también si DNS/TLS retrasan el envío.
        previousFinished = System.nanoTime();
        finished = true;
        semaphore.release();
    }
}
