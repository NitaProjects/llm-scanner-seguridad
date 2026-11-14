package com.llmscannerseguridad.application.execution;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class ScanBudget {
    private final LongSupplier ticker;
    private final long started;
    private final long available;
    private final UUID executionId;

    public ScanBudget(Instant scanStartedAt, long maximumMs, Clock clock, LongSupplier ticker) {
        this(scanStartedAt, maximumMs, clock.instant(), ticker.getAsLong(), ticker);
    }

    public ScanBudget(Instant scanStartedAt, long maximumMs, Instant enteredAt, long started, LongSupplier ticker) {
        this.ticker = ticker;
        this.started = started;
        long elapsed = Math.max(0, Duration.between(scanStartedAt, enteredAt).toMillis());
        available = Duration.ofMillis(Math.max(0, maximumMs - elapsed)).toNanos();
        executionId = null;
    }

    private ScanBudget(ScanBudget budget, UUID executionId) {
        ticker = budget.ticker;
        started = budget.started;
        available = budget.available;
        this.executionId = executionId;
    }

    ScanBudget forExecution(UUID id) { return new ScanBudget(this, id); }

    public UUID executionId() { return executionId; }

    public long remainingNanos() {
        return Math.max(0, available - Math.max(0, ticker.getAsLong() - started));
    }
}
