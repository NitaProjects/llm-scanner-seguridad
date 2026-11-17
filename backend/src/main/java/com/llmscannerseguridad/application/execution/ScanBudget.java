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
    private final String testCode;

    public ScanBudget(Instant scanStartedAt, long maximumMs, Clock clock, LongSupplier ticker) {
        this(scanStartedAt, maximumMs, clock.instant(), ticker.getAsLong(), ticker);
    }

    public ScanBudget(Instant scanStartedAt, long maximumMs, Instant enteredAt, long started, LongSupplier ticker) {
        this.ticker = ticker;
        this.started = started;
        long elapsed = Math.max(0, Duration.between(scanStartedAt, enteredAt).toMillis());
        available = Duration.ofMillis(Math.max(0, maximumMs - elapsed)).toNanos();
        executionId = null;
        testCode = null;
    }

    private ScanBudget(ScanBudget budget, UUID executionId, String testCode) {
        ticker = budget.ticker;
        started = budget.started;
        available = budget.available;
        this.executionId = executionId;
        this.testCode = testCode;
    }

    ScanBudget forExecution(UUID id, String testCode) { return new ScanBudget(this, id, testCode); }

    public UUID executionId() { return executionId; }
    public String testCode() { return testCode; }

    public long remainingNanos() {
        return Math.max(0, available - Math.max(0, ticker.getAsLong() - started));
    }
}
