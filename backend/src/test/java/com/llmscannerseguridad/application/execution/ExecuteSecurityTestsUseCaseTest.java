package com.llmscannerseguridad.application.execution;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.execution.*;
import com.llmscannerseguridad.domain.securitytest.*;
import com.llmscannerseguridad.domain.target.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExecuteSecurityTestsUseCaseTest {
    private final Instant now = Instant.parse("2026-10-10T10:00:00Z");
    private final UUID scan = UUID.randomUUID();
    private final TestExecutionRepositoryPort repository = mock(TestExecutionRepositoryPort.class);
    private final TargetInvocationPort invocation = mock(TargetInvocationPort.class);

    @Test
    void committedCompletePlanPrecedesAllRequestsAndSnapshotNeverChanges() {
        var plan = plan();
        when(repository.prepare(scan)).thenReturn(plan);
        when(repository.allCompleted(scan)).thenReturn(true);
        for (var row : plan.executions()) when(repository.start(row.id())).thenReturn(row.start(now, now));
        when(invocation.invoke(eq(plan.target()), anyString(), any())).thenReturn(success());
        worker().execute(scan);
        var order = inOrder(repository, invocation);
        order.verify(repository).prepare(scan);
        for (int i = 0; i < 4; i++) {
            var row = plan.executions().get(i);
            order.verify(repository).start(row.id());
            order.verify(invocation).invoke(eq(plan.target()), eq(plan.tests().get(i).prompt()), any());
            order.verify(repository).finish(eq(row.start(now, now)), eq(success()));
        }
        order.verify(repository).allCompleted(scan);
        verify(invocation, times(4)).invoke(same(plan.target()), anyString(), any());
    }

    @Test
    void individualFailuresContinueButAnyFailurePreventsSuccess() {
        var plan = stubPlan();
        var failure = InvocationResult.failure(ExecutionError.RESPONSE_INVALID_JSON);
        when(invocation.invoke(any(), anyString(), any())).thenReturn(failure, success(), success(), success());
        assertThatThrownBy(() -> worker().execute(scan)).isInstanceOf(ScanOperationException.class);
        verify(invocation, times(4)).invoke(any(), anyString(), any());
        verify(repository, times(4)).finish(any(), any());
    }

    @Test
    void globalAndPersistenceFailuresNeverSendRemainingTests() {
        for (var error : List.of(ExecutionError.TARGET_ADDRESS_BLOCKED, ExecutionError.CREDENTIAL_UNAVAILABLE,
                ExecutionError.SCAN_BUDGET_EXCEEDED, ExecutionError.EXECUTION_INTERNAL_ERROR)) {
            reset(repository, invocation);
            stubPlan();
            when(invocation.invoke(any(), anyString(), any())).thenReturn(InvocationResult.failure(error));
            assertThatThrownBy(() -> worker().execute(scan)).isInstanceOf(ScanOperationException.class);
            verify(invocation, times(1)).invoke(any(), anyString(), any());
        }
        reset(repository, invocation);
        stubPlan();
        when(invocation.invoke(any(), anyString(), any())).thenReturn(success());
        doThrow(ScanOperationException.internal()).when(repository).finish(any(), any());
        assertThatThrownBy(() -> worker().execute(scan)).isInstanceOf(ScanOperationException.class);
        verify(invocation, times(1)).invoke(any(), anyString(), any());
        reset(repository, invocation);
        when(repository.prepare(scan)).thenThrow(ScanOperationException.internal());
        assertThatThrownBy(() -> worker().execute(scan)).isInstanceOf(ScanOperationException.class);
        verifyNoInteractions(invocation);
    }

    @Test
    void incompleteUnknownRevisionAndDuplicateCatalogCannotBecomeAPlan() {
        var plan = plan();
        assertThatThrownBy(() -> new ExecutionPlan(List.of(), List.of(), plan.target(), now))
                .isInstanceOf(IllegalStateException.class);
        var changed = new ArrayList<>(plan.tests());
        changed.set(0, new SecurityTest(changed.getFirst().id(), changed.getFirst().code(), 2, "test", SecurityCategory.LLM01, "test"));
        assertThatThrownBy(() -> new ExecutionPlan(changed, plan.executions(), plan.target(), now))
                .isInstanceOf(IllegalStateException.class);
        changed.set(0, changed.get(1));
        assertThatThrownBy(() -> new ExecutionPlan(changed, plan.executions(), plan.target(), now))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void budgetIncludesTimeSinceScanStartedAndCannotBeExtendedByClockRegression() {
        var nanos = new AtomicLong(100);
        var budget = new ScanBudget(now.minusSeconds(299), 300000, Clock.fixed(now, ZoneOffset.UTC), nanos::get);
        assertThat(budget.remainingNanos()).isEqualTo(1000000000);
        nanos.addAndGet(1000000000);
        assertThat(budget.remainingNanos()).isZero();
        var expired = new ScanBudget(now.minusSeconds(301), 300000, Clock.fixed(now, ZoneOffset.UTC), nanos::get);
        assertThat(expired.remainingNanos()).isZero();
    }

    @Test
    void planPreparationConsumesMonotonicBudgetBeforeFirstRequest() {
        var nanos = new AtomicLong(100);
        var plan = plan();
        when(repository.prepare(scan)).thenAnswer(ignored -> {
            nanos.addAndGet(301000000000L);
            return plan;
        });
        var first = plan.executions().getFirst();
        when(repository.start(first.id())).thenReturn(first.start(now, now));
        var worker = new ExecuteSecurityTestsUseCase(repository, invocation, 300000,
                Clock.fixed(now.minusSeconds(10), ZoneOffset.UTC), nanos::get);
        assertThatThrownBy(() -> worker.execute(scan)).isInstanceOf(ScanOperationException.class);
        verifyNoInteractions(invocation);
        verify(repository).finish(eq(first.start(now, now)),
                eq(InvocationResult.failure(ExecutionError.SCAN_BUDGET_EXCEEDED)));
        verify(repository, times(1)).start(any());
    }

    private ExecutionPlan stubPlan() {
        var plan = plan();
        when(repository.prepare(scan)).thenReturn(plan);
        for (var row : plan.executions()) when(repository.start(row.id())).thenReturn(row.start(now, now));
        return plan;
    }

    private ExecuteSecurityTestsUseCase worker() {
        return new ExecuteSecurityTestsUseCase(repository, invocation, 300000, Clock.fixed(now, ZoneOffset.UTC), System::nanoTime);
    }

    private InvocationResult success() {
        return new InvocationResult(200, 1L, null, "{}", "untrusted", false, false);
    }

    private ExecutionPlan plan() {
        var target = new TargetConfiguration(UUID.randomUUID(), UUID.randomUUID(),
                new TargetSettings(null, new TargetUrl("https://controlled.invalid"), "{\"m\":\"{{PROMPT}}\"}",
                    "$.text", Map.of(), AuthType.NONE, null, 10000), null, now, now);
        var codes = List.of("LLM01-INSTRUCTION-001", "LLM02-CANARY-001", "LLM08-CONTEXT-001", "LLM10-MARKUP-001");
        var tests = codes.stream().map(code -> new SecurityTest(UUID.randomUUID(), code, 1, "test",
                SecurityCategory.valueOf(code.substring(0, 5)), "synthetic-" + code)).toList();
        var rows = tests.stream().map(test -> TestExecution.pending(UUID.randomUUID(), scan, test.id(), target.id(), now)).toList();
        return new ExecutionPlan(tests, rows, target, now);
    }
}
