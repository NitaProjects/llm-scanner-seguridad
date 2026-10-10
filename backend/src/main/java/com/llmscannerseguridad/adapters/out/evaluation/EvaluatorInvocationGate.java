package com.llmscannerseguridad.adapters.out.evaluation;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import com.llmscannerseguridad.application.evaluation.EvaluationCallBudget;

public final class EvaluatorInvocationGate {
    @FunctionalInterface public interface Pause { void sleep(long nanos) throws InterruptedException; }
    private final Semaphore permit = new Semaphore(1, true);
    private final long intervalNanos;
    private final LongSupplier ticker;
    private final Pause pause;
    private long lastStart;
    private boolean started;
    public EvaluatorInvocationGate(int intervalMs) {
        this(intervalMs, System::nanoTime, nanos -> TimeUnit.NANOSECONDS.sleep(nanos));
    }
    public EvaluatorInvocationGate(int intervalMs, LongSupplier ticker, Pause pause) {
        intervalNanos = intervalMs * 1000000L; this.ticker = ticker; this.pause = pause;
    }
    public boolean acquire(EvaluationCallBudget budget) throws InterruptedException {
        if (!permit.tryAcquire(budget.remainingNanos(), TimeUnit.NANOSECONDS)) return false;
        boolean accepted = false;
        try {
            long wait = started ? intervalNanos - (ticker.getAsLong() - lastStart) : 0;
            while (wait > 0) {
                if (budget.remainingNanos() <= wait) return false;
                pause.sleep(wait);
                wait = intervalNanos - (ticker.getAsLong() - lastStart);
            }
            if (budget.remainingNanos() <= 0) return false;
            lastStart = ticker.getAsLong(); started = true; accepted = true;
            return true;
        } finally { if (!accepted) permit.release(); }
    }
    public void release() { permit.release(); }
}
