package com.llmscannerseguridad.adapters.out.http.target;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.adapters.out.execution.securitytest.LegacyRestTargetAdapter;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.execution.*;
import com.llmscannerseguridad.domain.securitytest.*;
import com.llmscannerseguridad.domain.target.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HttpTargetFailurePrecedenceTest {
    @ParameterizedTest
    @EnumSource(value = ExecutionError.class, names = "TARGET_CONNECTION_FAILED", mode = EnumSource.Mode.EXCLUDE)
    void expiredIndividualDeadlinePreservesAlreadyClassifiedErrors(ExecutionError error) {
        var result = translate(error, System.nanoTime() - 1, false, budget());
        assertThat(result.errorReason()).isEqualTo(error);
        if (error == ExecutionError.EVIDENCE_LIMIT_EXCEEDED) {
            assertThat(result.requestBody()).isNull();
            assertThat(result.requestRedacted()).isFalse();
        }
    }

    @Test
    void onlyUnclassifiedTransportFailureBecomesTimeoutAfterIndividualDeadline() {
        assertThat(translate(ExecutionError.TARGET_CONNECTION_FAILED, System.nanoTime() - 1, false, budget()).errorReason())
                .isEqualTo(ExecutionError.TARGET_TIMEOUT);
        assertThat(translate(ExecutionError.TARGET_CONNECTION_FAILED, System.nanoTime() + 60_000_000_000L, false, budget()).errorReason())
                .isEqualTo(ExecutionError.TARGET_CONNECTION_FAILED);
    }

    @ParameterizedTest
    @EnumSource(ExecutionError.class)
    void exhaustedScanBudgetKeepsPrecedenceOverAllIndividualErrors(ExecutionError error) {
        var expired = new ScanBudget(Instant.now().minusSeconds(301), 300000, Clock.systemUTC(), System::nanoTime);
        assertThat(translate(error, System.nanoTime() - 1, false, expired).errorReason())
                .isEqualTo(ExecutionError.SCAN_BUDGET_EXCEEDED);
        assertThat(translate(error, System.nanoTime() - 1, true, budget()).errorReason())
                .isEqualTo(ExecutionError.SCAN_BUDGET_EXCEEDED);
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionError.class, names = {"TARGET_ADDRESS_BLOCKED", "CREDENTIAL_UNAVAILABLE",
            "TARGET_CONFIGURATION_INVALID", "EXECUTION_INTERNAL_ERROR"})
    void workerAbortsOnPreservedGlobalErrorWithoutStartingRemainingExecutions(ExecutionError error) {
        var now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var scan = UUID.randomUUID();
        var target = new TargetConfiguration(UUID.randomUUID(), UUID.randomUUID(),
                new TargetSettings(null, new TargetUrl("https://controlled.invalid"), "{\"m\":\"{{PROMPT}}\"}",
                        "$.text", Map.of(), AuthType.NONE, null, 10000), null, now, now);
        var tests = List.of("LLM01-INSTRUCTION-001", "LLM02-CANARY-001", "LLM08-CONTEXT-001", "LLM10-MARKUP-001")
                .stream().map(code -> new SecurityTest(UUID.randomUUID(), code, 1, "test",
                        SecurityCategory.valueOf(code.substring(0, 5)), "synthetic-" + code)).toList();
        var rows = tests.stream().map(test -> TestExecution.pending(UUID.randomUUID(), scan, test.id(), target.id(), now)).toList();
        var repository = mock(TestExecutionRepositoryPort.class);
        var invocation = mock(TargetInvocationPort.class);
        when(repository.prepare(scan)).thenReturn(new ExecutionPlan(tests, rows, target, now));
        var first = rows.getFirst().start(now, now);
        when(repository.start(first.id())).thenReturn(first);
        var result = translate(error, System.nanoTime() - 1, false, budget());
        when(invocation.invoke(any(), anyString(), any())).thenReturn(result);
        var worker = new ExecuteSecurityTestsUseCase(repository, new LegacyRestTargetAdapter(invocation), 300000, Clock.fixed(now, ZoneOffset.UTC), System::nanoTime);
        assertThatThrownBy(() -> worker.execute(scan)).isInstanceOf(ScanOperationException.class);
        verify(repository).finish(first, result);
        verify(repository, times(1)).start(any());
        verify(invocation, times(1)).invoke(any(), anyString(), any());
        verify(repository, never()).allCompleted(any());
    }

    private InvocationResult translate(ExecutionError error, long deadline, boolean scanLimited, ScanBudget budget) {
        try (var adapter = new HttpTargetInvocationAdapter(
                new TargetDestinationPolicy(Set.of(), false, false, Set.of(), Set.of()), null,
                new com.llmscannerseguridad.adapters.out.validation.target.TargetJsonProcessor(null),
                null, ExecutionLimits.defaults())) {
            // Deadline vencido determinista, sin sleeps ni carreras de scheduler/red.
            return ReflectionTestUtils.invokeMethod(adapter, "failure", error, null, System.nanoTime(), deadline,
                    scanLimited, budget, "{}", true, java.util.function.UnaryOperator.<String>identity());
        }
    }

    private ScanBudget budget() {
        return new ScanBudget(Instant.now(), 300000, Clock.systemUTC(), System::nanoTime);
    }
}
