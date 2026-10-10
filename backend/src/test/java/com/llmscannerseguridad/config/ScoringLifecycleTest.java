package com.llmscannerseguridad.config;

import java.time.Clock;
import java.util.*;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.application.scoring.*;
import com.llmscannerseguridad.domain.evaluation.Verdict;
import com.llmscannerseguridad.domain.scan.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static com.llmscannerseguridad.application.scoring.ScoringFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScoringLifecycleTest {
    @Test void scoringFailureAndUnavailableCompensationCloseExistingGateWithoutSemanticFallback() {
        var lifecycle=mock(ScanLifecyclePort.class);var gate=new ScanExecutionGate();gate.recovered();
        when(lifecycle.claim(SCAN)).thenReturn(Optional.of(snapshot().scan()));
        when(lifecycle.complete(SCAN)).thenThrow(ScanOperationException.internal());
        when(lifecycle.fail(SCAN,ScanStatus.RUNNING,ScanFailureReason.SCAN_PROCESSING_FAILED)).thenThrow(ScanOperationException.internal());
        var work=mock(ScanWorkPort.class);
        new ProcessScanUseCase(lifecycle,work,gate).execute(SCAN);
        verify(work).execute(SCAN);verify(lifecycle).fail(SCAN,ScanStatus.RUNNING,ScanFailureReason.SCAN_PROCESSING_FAILED);
        assertThatThrownBy(()->gate.admit(()->true)).isInstanceOf(ScanOperationException.class);
        verifyNoMoreInteractions(work);
    }
    @Test void sourceFailureRollsBackBeforeScoreInsertOrScanTransition() {
        var delegate=mock(ScanLifecyclePort.class);var manager=mock(PlatformTransactionManager.class);
        var tx=new SimpleTransactionStatus();when(manager.getTransaction(any())).thenReturn(tx);
        var repository=mock(ScanScoreRepositoryPort.class);when(repository.lock(SCAN)).thenReturn(snapshot());
        var scans=mock(ScanRepositoryPort.class);var inputs=mock(ScoringInputPort.class);
        when(inputs.read(SCAN)).thenThrow(new IllegalStateException("SYNTHETIC_SOURCE_ERROR"));
        var lifecycle=new TransactionalScanLifecycle(delegate,manager,new ScanExecutionGate(),null,null,repository,new CalculateScanScoreUseCase(inputs,Clock.systemUTC()),scans);
        assertThatThrownBy(()->lifecycle.complete(SCAN)).isInstanceOf(ScanOperationException.class);
        verify(manager).rollback(tx);verify(manager,never()).commit(any());verify(repository,never()).insert(any());verifyNoInteractions(scans,delegate);
    }
    @Test void completedDuplicateDoesNotReadInputsOrRecalculateAndRecoveryIntegrityFailureKeepsGateClosed() {
        var data=inputs(Verdict.NOT_VULNERABLE);
        var score=new CalculateScanScoreUseCase(id->data,Clock.systemUTC()).execute(snapshot());
        var manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var repository=mock(ScanScoreRepositoryPort.class);
        when(repository.lock(SCAN)).thenReturn(new ScanScoreSnapshot(snapshot().scan().complete(score.calculatedAt()),snapshot().requiredVersion(),snapshot().evaluationProfile(),score));
        var inputs=mock(ScoringInputPort.class);var scans=mock(ScanRepositoryPort.class);var delegate=mock(ScanLifecyclePort.class);
        var gate=new ScanExecutionGate();
        var lifecycle=new TransactionalScanLifecycle(delegate,manager,gate,null,null,repository,new CalculateScanScoreUseCase(inputs,Clock.systemUTC()),scans);
        assertThat(lifecycle.complete(SCAN)).isTrue();verifyNoInteractions(inputs,scans,delegate);verify(repository,never()).insert(any());
        doThrow(ScanOperationException.internal()).when(repository).verifyRecoveryIntegrity();
        assertThatThrownBy(()->new ScanStartup(new RecoverInterruptedScansUseCase(lifecycle),gate).initialize()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->gate.admit(()->true)).isInstanceOf(ScanOperationException.class);
        verify(delegate,never()).recoverInterrupted();
    }
}
