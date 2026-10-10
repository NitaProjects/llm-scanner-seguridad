package com.llmscannerseguridad.application.evaluation;

import java.util.function.LongSupplier;

public final class EvaluationCallBudget {
    private final LongSupplier remainingScanNanos;
    private final LongSupplier ticker;
    private final long deadline;
    public EvaluationCallBudget(LongSupplier remainingScanNanos, long timeoutMs, LongSupplier ticker) {
        this.remainingScanNanos = remainingScanNanos;
        this.ticker = ticker;
        deadline = ticker.getAsLong() + Math.min(timeoutMs * 1000000L, remainingScanNanos.getAsLong());
    }
    public long remainingNanos() { return Math.max(0, Math.min(deadline - ticker.getAsLong(), remainingScanNanos.getAsLong())); }
    public boolean scanExpired() { return remainingScanNanos.getAsLong() <= 0; }
}
