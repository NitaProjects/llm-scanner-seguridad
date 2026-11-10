package com.llmscannerseguridad.adapters.out.execution.securitytest;

import java.time.Clock;
import java.time.Instant;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.TargetConfiguration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyRestTargetAdapterTest {
    private final TargetInvocationPort invocation = mock(TargetInvocationPort.class);
    private final TargetConfiguration target = mock(TargetConfiguration.class);
    private final ScanBudget budget = new ScanBudget(Instant.now(), 300000, Clock.systemUTC(), System::nanoTime);
    private final LegacyRestTargetAdapter bridge = new LegacyRestTargetAdapter(invocation);

    @Test
    void delegatesExactlyOnceWithSameObjectsAndPreservesFailureMetadata() {
        var result = new InvocationResult(429, 13L, ExecutionError.HTTP_STATUS_ERROR, "{}", "", true, false);
        when(invocation.invoke(target, " \0😀 ", budget)).thenReturn(result);
        assertThat(LegacyInvocationResultProjection.toLegacy(bridge.interact(target, " \0😀 ", budget)))
                .isEqualTo(result);
        verify(invocation).invoke(same(target), eq(" \0😀 "), same(budget));
        verifyNoMoreInteractions(invocation);
    }

    @Test
    void doesNotCatchExceptionOrErrorOrRetry() {
        var exception = new IllegalStateException("fixture");
        when(invocation.invoke(target, "prompt", budget)).thenThrow(exception);
        assertThatThrownBy(() -> bridge.interact(target, "prompt", budget)).isSameAs(exception);
        verify(invocation).invoke(same(target), eq("prompt"), same(budget));
        verifyNoMoreInteractions(invocation);
        reset(invocation);
        var error = new AssertionError("fixture");
        when(invocation.invoke(target, "prompt", budget)).thenThrow(error);
        assertThatThrownBy(() -> bridge.interact(target, "prompt", budget)).isSameAs(error);
        verify(invocation).invoke(same(target), eq("prompt"), same(budget));
        verifyNoMoreInteractions(invocation);
    }

    @Test
    void preservesInterruptionAndNullWithoutOwningDelegateLifecycle() {
        when(invocation.invoke(target, "prompt", budget)).thenAnswer(ignored -> {
            Thread.currentThread().interrupt();
            return null;
        });
        try {
            assertThat(bridge.interact(target, "prompt", budget)).isNull();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(AutoCloseable.class.isAssignableFrom(LegacyRestTargetAdapter.class)).isFalse();
            verify(invocation).invoke(same(target), eq("prompt"), same(budget));
            verifyNoMoreInteractions(invocation);
        } finally {
            Thread.interrupted();
        }
    }
}
