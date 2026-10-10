package com.llmscannerseguridad.config;

import java.time.Instant;
import java.util.UUID;
import com.llmscannerseguridad.application.execution.TestExecutionRepositoryPort;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.scan.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecurityTestExecutionTransactionsTest {
    private final UUID id = UUID.randomUUID();
    private final ScanLifecyclePort delegate = mock(ScanLifecyclePort.class);
    private final TestExecutionRepositoryPort executions = mock(TestExecutionRepositoryPort.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final SimpleTransactionStatus transaction = new SimpleTransactionStatus();

    private TransactionalScanLifecycle lifecycle() {
        when(manager.getTransaction(any())).thenReturn(transaction);
        return new TransactionalScanLifecycle(delegate, manager, new ScanExecutionGate(), executions);
    }

    @Test
    void scanAndChildrenShareOneTerminalTransactionAndFalseNeverTouchesChildren() {
        var lifecycle = lifecycle();
        when(delegate.fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED)).thenReturn(true);
        assertThat(lifecycle.fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED)).isTrue();
        var order = inOrder(delegate, executions, manager);
        order.verify(manager).getTransaction(any());
        order.verify(delegate).fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED);
        order.verify(executions).closeActive(eq(id), any(Instant.class), eq(ExecutionError.EXECUTION_ABORTED));
        order.verify(manager).commit(transaction);
        clearInvocations(delegate, executions, manager);
        when(delegate.fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED)).thenReturn(false);
        assertThat(lifecycle.fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED)).isFalse();
        verifyNoInteractions(executions);
    }

    @Test
    void childFailureRollsBackScanAndCommitFailureIsSanitized() {
        var lifecycle = lifecycle();
        when(delegate.fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED)).thenReturn(true);
        doThrow(new IllegalStateException("PRIVATE_CHILD_MARKER")).when(executions).closeActive(any(), any(), any());
        assertThatThrownBy(() -> lifecycle.fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED))
                .isInstanceOfSatisfying(ScanOperationException.class, error -> assertThat(error.getCause()).isNull());
        verify(manager).rollback(transaction);
        verify(manager, never()).commit(any());
        reset(executions, manager);
        when(manager.getTransaction(any())).thenReturn(transaction);
        doThrow(new TransactionSystemException("PRIVATE_COMMIT_MARKER")).when(manager).commit(any());
        assertThatThrownBy(() -> lifecycle.fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED))
                .satisfies(error -> {
                    assertThat(error.getCause()).isNull();
                    assertThat(error.getMessage()).doesNotContain("PRIVATE_COMMIT_MARKER");
                });
    }

    @Test
    void recoveryCommitsTogetherAndCompletionRequiresPersistedNonEmptySuccess() {
        var lifecycle = lifecycle();
        when(delegate.recoverInterrupted()).thenReturn(2);
        assertThat(lifecycle.recoverInterrupted()).isEqualTo(2);
        var order = inOrder(executions, delegate, manager);
        order.verify(manager).getTransaction(any());
        order.verify(executions).recoverActive(any());
        order.verify(delegate).recoverInterrupted();
        order.verify(manager).commit(transaction);
        assertThatThrownBy(() -> lifecycle.complete(id)).isInstanceOf(ScanOperationException.class);
        verify(delegate, never()).complete(any());
        when(executions.allCompleted(id)).thenReturn(true);
        when(delegate.complete(id)).thenReturn(true);
        assertThat(lifecycle.complete(id)).isTrue();
    }
}
