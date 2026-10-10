package com.llmscannerseguridad.adapters.out.http.target;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.application.execution.ScanBudget;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ExecutionLimitsAndGateTest {
    @Test
    void limitsAreFiniteAndCanBeReducedWithinApprovedMargins() {
        assertThat(ExecutionLimits.defaults().scanBudgetMs()).isEqualTo(300000);
        assertThat(new ExecutionLimits(4, 1024, 4096, 1024, 4096, 16384, 60000, 500, 1000, 1200).requestBytes()).isEqualTo(1024);
        assertThatThrownBy(() -> new ExecutionLimits(11, 1, 1, 1, 1, 1, 60000, 1, 1, 1000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InvocationGate(999)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExecutionLimits(4, 65537, 1, 1, 1, 1, 60000, 1, 1, 1000)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atLeastOneSecondBetweenStartsAndExpiredQuotaNeverAdmits() {
        var gate = new InvocationGate(1000);
        var budget = new ScanBudget(Instant.now(), 60000, Clock.systemUTC(), System::nanoTime);
        gate.acquire(budget);
        long first = System.nanoTime();
        gate.release();
        gate.acquire(budget);
        long second = System.nanoTime();
        gate.release();
        assertThat(second - first).isGreaterThanOrEqualTo(999000000L);
        var expired = new ScanBudget(Instant.now().minusSeconds(61), 60000, Clock.systemUTC(), System::nanoTime);
        assertThatThrownBy(() -> gate.acquire(expired)).isInstanceOf(InvocationFailure.class);
    }

    @Test
    void secondCallerCannotAcquireWhileOneInvocationIsInFlight() throws Exception {
        var gate = new InvocationGate(1000);
        gate.acquire(new ScanBudget(Instant.now(), 60000, Clock.systemUTC(), System::nanoTime));
        try (var caller = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            caller.submit(() -> {
                var shortBudget = new ScanBudget(Instant.now(), 50, Clock.systemUTC(), System::nanoTime);
                assertThatThrownBy(() -> gate.acquire(shortBudget)).isInstanceOf(InvocationFailure.class);
            }).get(2, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            gate.release();
        }
        gate.acquire(new ScanBudget(Instant.now(), 60000, Clock.systemUTC(), System::nanoTime));
        gate.release();
    }
}
