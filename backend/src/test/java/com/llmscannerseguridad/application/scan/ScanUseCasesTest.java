package com.llmscannerseguridad.application.scan;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort;
import com.llmscannerseguridad.domain.project.Project;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanFailureReason;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScanUseCasesTest {
    private final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    private final TargetConfigurationRepositoryPort targets = mock(TargetConfigurationRepositoryPort.class);
    private final ScanRepositoryPort scans = mock(ScanRepositoryPort.class);
    private final ScanLifecyclePort lifecycle = mock(ScanLifecyclePort.class);
    private final ScanDispatcherPort dispatcher = mock(ScanDispatcherPort.class);
    private final ScanWorkPort work = mock(ScanWorkPort.class);
    private final ScanFailureHandlerPort failures = mock(ScanFailureHandlerPort.class);
    private final UUID project = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-10T08:00:00.123456Z");
    private final Scan pending = Scan.pending(id, project, now);
    private ScanLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new ScanLifecycleService(projects, targets, scans, Clock.fixed(now, ZoneOffset.UTC), () -> id);
        when(projects.findById(project)).thenReturn(Optional.of(new Project(project, "Proyecto", null, now, now)));
        when(targets.existsByProjectId(project)).thenReturn(true);
    }

    @Test
    void createsPendingAfterPreconditionsWithoutLoadingTarget() {
        assertThat(service.create(project)).isEqualTo(pending);
        var order = inOrder(projects, targets, scans);
        order.verify(projects).findById(project);
        order.verify(targets).existsByProjectId(project);
        order.verify(scans).existsActiveByProjectId(project);
        order.verify(scans).insert(pending);
        verify(targets, never()).findByProjectId(any());
    }

    @Test
    void preconditionsAndStorageFailuresNeverCreateOrDispatch() {
        when(projects.findById(project)).thenReturn(Optional.empty());
        assertReason(() -> service.create(project), ScanOperationException.Reason.PROJECT_NOT_FOUND);
        verifyNoInteractions(targets, scans, dispatcher);
        when(projects.findById(project)).thenReturn(Optional.of(new Project(project, "Proyecto", null, now, now)));
        when(targets.existsByProjectId(project)).thenReturn(false);
        assertReason(() -> service.create(project), ScanOperationException.Reason.TARGET_NOT_CONFIGURED);
        verifyNoInteractions(scans, dispatcher);
        when(targets.existsByProjectId(project)).thenReturn(true);
        when(scans.existsActiveByProjectId(project)).thenReturn(true);
        assertReason(() -> service.create(project), ScanOperationException.Reason.SCAN_ALREADY_ACTIVE);
        verify(scans, never()).insert(any());
        when(lifecycle.create(project)).thenThrow(ScanOperationException.internal());
        assertReason(() -> new StartScanUseCase(lifecycle, dispatcher, failures).execute(project),
                ScanOperationException.Reason.INTERNAL_ERROR);
        verifyNoInteractions(dispatcher);
    }

    @Test
    void preservesInitialSnapshotEvenIfDispatcherFinishesImmediately() {
        when(lifecycle.create(project)).thenReturn(pending);
        doAnswer(invocation -> {
            when(lifecycle.claim(id)).thenReturn(Optional.of(pending.start(now)));
            when(lifecycle.complete(id)).thenReturn(true);
            new ProcessScanUseCase(lifecycle, work, failures).execute(id);
            return null;
        }).when(dispatcher).dispatch(id);
        assertThat(new StartScanUseCase(lifecycle, dispatcher, failures).execute(project)).isEqualTo(pending);
        var order = inOrder(lifecycle, dispatcher, work);
        order.verify(lifecycle).create(project);
        order.verify(dispatcher).dispatch(id);
        order.verify(lifecycle).claim(id);
        order.verify(work).execute(id);
        order.verify(lifecycle).complete(id);
    }

    @Test
    void dispatchRejectionCompensatesAndKeepsLocationIdentityEvenIfCompensationFails() {
        when(lifecycle.create(project)).thenReturn(pending);
        doThrow(new IllegalStateException("MARCADOR_INTERNO_DISPATCH")).when(dispatcher).dispatch(id);
        when(lifecycle.fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_DISPATCH_FAILED)).thenReturn(true);
        var start = new StartScanUseCase(lifecycle, dispatcher, failures);
        assertThatThrownBy(() -> start.execute(project)).isInstanceOfSatisfying(ScanOperationException.class, error -> {
            assertThat(error.reason()).isEqualTo(ScanOperationException.Reason.SCAN_DISPATCH_FAILED);
            assertThat(error.scanId()).isEqualTo(id);
            assertThat(error.projectId()).isEqualTo(project);
            assertThat(error.getCause()).isNull();
        });
        verifyNoInteractions(failures);
        when(lifecycle.fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_DISPATCH_FAILED)).thenThrow(new IllegalStateException("MARCADOR_DB"));
        assertThatThrownBy(() -> start.execute(project)).isInstanceOfSatisfying(ScanOperationException.class, error -> {
            assertThat(error.reason()).isEqualTo(ScanOperationException.Reason.INTERNAL_ERROR);
            assertThat(error.scanId()).isEqualTo(id);
        });
        verify(failures).persistenceFailed(id);
    }

    @Test
    void conditionalClaimAndTerminalTransitionsIgnoreDuplicates() {
        when(scans.findById(id)).thenReturn(Optional.of(pending));
        when(scans.transition(eq(ScanStatus.PENDING), any())).thenReturn(false);
        assertThat(service.claim(id)).isEmpty();
        when(scans.transition(eq(ScanStatus.PENDING), any())).thenReturn(true);
        assertThat(service.claim(id)).contains(pending.start(now));
        when(scans.findById(id)).thenReturn(Optional.of(pending.start(now).complete(now)));
        assertThat(service.claim(id)).isEmpty();
        assertThat(service.complete(id)).isFalse();
        assertThat(service.fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED)).isFalse();
        when(lifecycle.claim(id)).thenReturn(Optional.empty());
        new ProcessScanUseCase(lifecycle, work, failures).execute(id);
        verifyNoInteractions(work, failures);
    }

    @Test
    void failedClaimCannotFailRunningWorkClaimedByAnotherDelivery() {
        when(scans.findById(id)).thenReturn(Optional.of(pending.start(now)));
        assertThat(service.fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_PROCESSING_FAILED)).isFalse();
        verify(scans, never()).transition(any(), any());
    }

    @Test
    void workerFailureAndFailedRunningCommitDoNotExposeInternalMessages() {
        var process = new ProcessScanUseCase(lifecycle, work, failures);
        when(lifecycle.claim(id)).thenThrow(ScanOperationException.internal());
        when(lifecycle.fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_PROCESSING_FAILED)).thenReturn(true);
        process.execute(id);
        verifyNoInteractions(work);
        verify(lifecycle).fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_PROCESSING_FAILED);
        reset(lifecycle);
        when(lifecycle.claim(id)).thenReturn(Optional.of(pending.start(now)));
        doThrow(new IllegalStateException("MARCADOR_INTERNO_WORKER")).when(work).execute(id);
        process.execute(id);
        verify(lifecycle).fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED);
        verify(lifecycle, never()).complete(any());
        verifyNoInteractions(failures);
    }

    @Test
    void uncertainClaimNeverFailsRunningClaimAndBlocksWhenPendingCompensationDoesNotApply() {
        when(lifecycle.claim(id)).thenThrow(ScanOperationException.internal());
        when(lifecycle.fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_PROCESSING_FAILED)).thenReturn(false);
        new ProcessScanUseCase(lifecycle, work, failures).execute(id);
        verifyNoInteractions(work);
        verify(lifecycle).fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_PROCESSING_FAILED);
        verify(lifecycle, never()).fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED);
        verify(lifecycle, never()).complete(any());
        verify(failures).persistenceFailed(id);
    }

    @Test
    void failedCompletedCommitAttemptsFailedOnceThenBlocksOnPersistenceFailure() {
        when(lifecycle.claim(id)).thenReturn(Optional.of(pending.start(now)));
        when(lifecycle.complete(id)).thenThrow(ScanOperationException.internal());
        when(lifecycle.fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED)).thenThrow(ScanOperationException.internal());
        new ProcessScanUseCase(lifecycle, work, failures).execute(id);
        verify(work).execute(id);
        verify(lifecycle).complete(id);
        verify(lifecycle).fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED);
        verify(failures).persistenceFailed(id);
        reset(lifecycle, work, failures);
        when(lifecycle.claim(id)).thenThrow(new ScanOperationException(ScanOperationException.Reason.SCAN_EXECUTION_UNAVAILABLE));
        new ProcessScanUseCase(lifecycle, work, failures).execute(id);
        verifyNoInteractions(work, failures);
        verify(lifecycle, never()).fail(any(), any(), any());
    }

    @Test
    void queriesCheckProjectFirstUseScopedLookupAndNeverTouchTargetOrDispatch() {
        var get = new GetScanUseCase(projects, scans);
        var list = new ListProjectScansUseCase(projects, scans);
        when(scans.findByProjectIdAndId(project, id)).thenReturn(Optional.of(pending));
        when(scans.findByProjectIdOrdered(project)).thenReturn(List.of(pending));
        assertThat(get.execute(project, id)).isEqualTo(pending);
        assertThat(list.execute(project)).containsExactly(pending);
        when(scans.findByProjectIdAndId(project, id)).thenReturn(Optional.empty());
        assertReason(() -> get.execute(project, id), ScanOperationException.Reason.SCAN_NOT_FOUND);
        when(scans.findByProjectIdOrdered(project)).thenReturn(List.of());
        assertThat(list.execute(project)).isEmpty();
        when(scans.findByProjectIdOrdered(project)).thenThrow(ScanOperationException.internal());
        assertReason(() -> list.execute(project), ScanOperationException.Reason.INTERNAL_ERROR);
        reset(scans);
        when(projects.findById(project)).thenReturn(Optional.empty());
        assertReason(() -> get.execute(project, id), ScanOperationException.Reason.PROJECT_NOT_FOUND);
        assertReason(() -> list.execute(project), ScanOperationException.Reason.PROJECT_NOT_FOUND);
        verifyNoInteractions(scans, targets, dispatcher, work);
    }

    @Test
    void recoveryOnlyChangesActiveRowsAndClampsRegressingClock() {
        var running = Scan.pending(UUID.randomUUID(), UUID.randomUUID(), now.plusSeconds(2)).start(now.plusSeconds(3));
        when(scans.findActive()).thenReturn(List.of(pending, running));
        when(scans.transition(any(), any())).thenReturn(true);
        assertThat(service.recoverInterrupted()).isEqualTo(2);
        verify(scans).transition(ScanStatus.PENDING, pending.fail(ScanFailureReason.SCAN_INTERRUPTED, now));
        verify(scans).transition(ScanStatus.RUNNING, running.fail(ScanFailureReason.SCAN_INTERRUPTED, now));
        verifyNoInteractions(dispatcher, work);
    }

    private void assertReason(Runnable operation, ScanOperationException.Reason reason) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(ScanOperationException.class,
                error -> assertThat(error.reason()).isEqualTo(reason));
    }
}
