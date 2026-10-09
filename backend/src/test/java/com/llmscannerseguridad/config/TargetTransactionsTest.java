package com.llmscannerseguridad.config;

import com.llmscannerseguridad.application.target.TargetOperationException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TargetTransactionsTest {
    @Test
    void commitPrecedesSuccessAndCommitFailuresAreSanitized() {
        var manager = mock(PlatformTransactionManager.class);
        var status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        var transactions = new TargetTransactions(manager);
        assertThat(transactions.execute(() -> "resultado")).isEqualTo("resultado");
        verify(manager).getTransaction(argThat(def ->
                def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(manager).commit(status);
        doThrow(new TransactionSystemException("detalle interno")).when(manager).commit(status);
        assertThatThrownBy(() -> transactions.execute(() -> "no devolver"))
                .isInstanceOfSatisfying(TargetOperationException.class, error -> {
                    assertThat(error.reason()).isEqualTo(TargetOperationException.Reason.INTERNAL_ERROR);
                    assertThat(error.getCause()).isNull();
                });
    }

    @Test
    void failureRollsBackAndPreservesFunctionalConflict() {
        var manager = mock(PlatformTransactionManager.class);
        var status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        var conflict = new TargetOperationException(TargetOperationException.Reason.TARGET_ALREADY_CONFIGURED);
        assertThatThrownBy(() -> new TargetTransactions(manager).execute(() -> { throw conflict; })).isSameAs(conflict);
        verify(manager).rollback(status);
        verify(manager, never()).commit(any());
    }
}
