package com.llmscannerseguridad.config;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.llmscannerseguridad.application.scan.ProcessScanUseCase;
import com.llmscannerseguridad.application.scan.RecoverInterruptedScansUseCase;
import com.llmscannerseguridad.application.scan.ScanDispatcherPort;
import com.llmscannerseguridad.application.scan.ScanLifecyclePort;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.application.scan.ScanWorkPort;
import com.llmscannerseguridad.application.scan.StartScanUseCase;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanFailureReason;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.GenericApplicationContext;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScanInfrastructureTest {
    private final UUID id = UUID.randomUUID();
    private final UUID project = UUID.randomUUID();
    private final Scan pending = Scan.pending(id, project, Instant.parse("2026-10-10T08:00:00Z"));

    @Test
    void newTransactionsCommitBeforeDispatchAndFailedCommitNeverLaunches() {
        var delegate = mock(ScanLifecyclePort.class);
        var manager = mock(PlatformTransactionManager.class);
        var tx = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenAnswer(invocation -> {
            assertThat(invocation.<TransactionDefinition>getArgument(0).getPropagationBehavior())
                    .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return tx;
        });
        when(delegate.create(project)).thenReturn(pending);
        var gate = new ScanExecutionGate();
        gate.recovered();
        var lifecycle = new TransactionalScanLifecycle(delegate, manager, gate);
        var dispatcher = mock(ScanDispatcherPort.class);
        var start = new StartScanUseCase(lifecycle, dispatcher, gate);
        assertThat(start.execute(project)).isEqualTo(pending);
        var order = inOrder(delegate, manager, dispatcher);
        order.verify(manager).getTransaction(any());
        order.verify(delegate).create(project);
        order.verify(manager).commit(tx);
        order.verify(dispatcher).dispatch(id);
        clearInvocations(dispatcher);
        doThrow(new TransactionSystemException("MARCADOR_COMMIT_PRIVADO")).when(manager).commit(tx);
        assertThatThrownBy(() -> start.execute(project)).isInstanceOfSatisfying(ScanOperationException.class,
                error -> assertThat(error.reason()).isEqualTo(ScanOperationException.Reason.INTERNAL_ERROR));
        verifyNoInteractions(dispatcher);
    }

    @Test
    void concreteConstraintIsTranslatedEvenWhenOnlyCommitFails() {
        var delegate = mock(ScanLifecyclePort.class);
        var manager = mock(PlatformTransactionManager.class);
        var tx = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(tx);
        when(delegate.create(project)).thenReturn(pending);
        var gate = new ScanExecutionGate();
        gate.recovered();
        var lifecycle = new TransactionalScanLifecycle(delegate, manager, gate);
        for (String constraint : new String[] {"uq_scans_active_project", "scans_pkey"}) {
            var violation = new ConstraintViolationException("MARCADOR_SQL", new SQLException("MARCADOR_SQL", "23505"), constraint);
            doThrow(new TransactionSystemException("MARCADOR_COMMIT", violation)).when(manager).commit(tx);
            assertThatThrownBy(() -> lifecycle.create(project)).isInstanceOfSatisfying(ScanOperationException.class,
                    error -> {
                        assertThat(error.reason()).isEqualTo(constraint.equals("uq_scans_active_project")
                                ? ScanOperationException.Reason.SCAN_ALREADY_ACTIVE : ScanOperationException.Reason.INTERNAL_ERROR);
                        assertThat(error.getCause()).isNull();
                    });
        }
    }

    @Test
    void failedRunningCommitPreventsWorkAndCompensatesInAnotherTransaction() {
        var delegate = mock(ScanLifecyclePort.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        when(delegate.claim(id)).thenReturn(Optional.of(pending.start(pending.createdAt())));
        when(delegate.fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_PROCESSING_FAILED)).thenReturn(true);
        var commits = new AtomicInteger();
        doAnswer(invocation -> {
            if (commits.incrementAndGet() == 1) {
                throw new TransactionSystemException("MARCADOR_COMMIT_RUNNING");
            }
            return null;
        }).when(manager).commit(any());
        var gate = new ScanExecutionGate();
        gate.recovered();
        var work = mock(ScanWorkPort.class);
        new ProcessScanUseCase(new TransactionalScanLifecycle(delegate, manager, gate), work, gate).execute(id);
        verifyNoInteractions(work);
        verify(delegate).fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_PROCESSING_FAILED);
        assertThat(commits).hasValue(2);
    }

    @Test
    void recoveryFailureLeavesGateClosedAndPersistentFailureBlocksNewRowsAndClaimsOnly() {
        var delegate = mock(ScanLifecyclePort.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        var gate = new ScanExecutionGate();
        var lifecycle = new TransactionalScanLifecycle(delegate, manager, gate);
        when(delegate.recoverInterrupted()).thenThrow(ScanOperationException.internal());
        assertThatThrownBy(() -> new ScanStartup(new RecoverInterruptedScansUseCase(lifecycle), gate).initialize())
                .satisfies(error -> assertThat(new ScanRecoveryFailureDiagnostics(null).reportException(error)).isTrue());
        assertUnavailable(() -> lifecycle.create(project));
        verify(delegate, never()).create(any());
        doReturn(2).when(delegate).recoverInterrupted();
        new ScanStartup(new RecoverInterruptedScansUseCase(lifecycle), gate).initialize();
        gate.persistenceFailed(id);
        assertUnavailable(() -> lifecycle.create(project));
        assertUnavailable(() -> lifecycle.claim(id));
        verify(delegate, never()).claim(any());
        lifecycle.complete(id);
        verify(delegate).complete(id);
    }

    @Test
    void runningCommitAppliedButAcknowledgementLostPreservesDurableClaimAndBlocksAdmission() {
        var delegate = mock(ScanLifecyclePort.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        var running = pending.start(pending.createdAt());
        var durable = new AtomicReference<>(pending);
        when(delegate.claim(id)).thenReturn(Optional.of(running));
        when(delegate.fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_PROCESSING_FAILED))
                .thenAnswer(invocation -> durable.get().status() == ScanStatus.PENDING);
        var commits = new AtomicInteger();
        doAnswer(invocation -> {
            if (commits.incrementAndGet() == 1) {
                durable.set(running);
                throw new TransactionSystemException("MARCADOR_CONFIRMACION_JDBC_PERDIDA");
            }
            return null;
        }).when(manager).commit(any());
        var gate = new ScanExecutionGate();
        gate.recovered();
        var lifecycle = new TransactionalScanLifecycle(delegate, manager, gate);
        var work = mock(ScanWorkPort.class);
        new ProcessScanUseCase(lifecycle, work, gate).execute(id);
        verifyNoInteractions(work);
        assertThat(durable).hasValue(running);
        verify(delegate, never()).fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED);
        assertUnavailable(() -> lifecycle.create(project));
        assertUnavailable(() -> lifecycle.claim(UUID.randomUUID()));
        verify(delegate, never()).create(any());
        // Una entrega legítima ya RUNNING conserva la posibilidad de finalizar.
        lifecycle.complete(id);
        verify(delegate).complete(id);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void fullBootContextRecoveryFailureLogsOnlyControlledDiagnosticAndStops(CapturedOutput output) {
        var lifecycle = mock(ScanLifecyclePort.class);
        when(lifecycle.recoverInterrupted()).thenThrow(new IllegalStateException(
                "MARCADOR_SECRETO SELECT * FROM scans C:\\ruta\\privada org.postgresql.Driver"));
        var gate = new ScanExecutionGate();
        assertThatThrownBy(() -> new SpringApplicationBuilder(RecoveryApplication.class)
                .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).logStartupInfo(false)
                .registerShutdownHook(false).initializers(context -> {
                    var beans = (GenericApplicationContext) context;
                    beans.registerBean(ScanLifecyclePort.class, () -> lifecycle);
                    beans.registerBean(ScanExecutionGate.class, () -> gate);
                    beans.registerBean(ScanStartup.class,
                            () -> new ScanStartup(new RecoverInterruptedScansUseCase(lifecycle), gate),
                            definition -> definition.setInitMethodName("initialize"));
                }).run()).isInstanceOf(RuntimeException.class);
        assertUnavailable(() -> gate.admit(() -> null));
        assertThat(output.getAll()).contains("SCAN_RECOVERY_FAILED", "Backend detenido")
                .doesNotContain("Application run failed", "BeanCreationException", "RecoveryFailure",
                        "MARCADOR_SECRETO", "SELECT", "C:\\", "org.postgresql", "at com.", "at org.",
                        "Error creating bean", "defined in", "Caused by:");
        verify(lifecycle).recoverInterrupted();
    }

    @Test
    void recoveryReporterDoesNotHandleOtherStartupErrorsOrRetainItsFilter() {
        var context = (ch.qos.logback.classic.LoggerContext) LoggerFactory.getILoggerFactory();
        var before = context.getTurboFilterList().size();
        var reporter = new ScanRecoveryFailureDiagnostics(null);
        var unrelated = new IllegalStateException("Fallo ajeno a recuperación");
        assertThat(reporter.reportException(unrelated)).isFalse();
        var failure = ScanRecoveryFailureDiagnostics.failure();
        assertThat(context.getTurboFilterList()).hasSize(before + 1);
        assertThat(reporter.reportException(failure)).isTrue();
        assertThat(context.getTurboFilterList()).hasSize(before);
        assertThat(reporter.reportException(unrelated)).isFalse();
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void unrelatedBootStartupFailureKeepsItsNormalDiagnostic(CapturedOutput output) {
        assertThatThrownBy(() -> new SpringApplicationBuilder(RecoveryApplication.class)
                .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).logStartupInfo(false)
                .registerShutdownHook(false).initializers(context ->
                        ((GenericApplicationContext) context).registerBean("unrelatedBean", Object.class, () -> {
                            throw new IllegalStateException("MARCADOR_FALLO_AJENO");
                        })).run()).isInstanceOf(RuntimeException.class);
        assertThat(output.getAll()).contains("Application run failed", "MARCADOR_FALLO_AJENO")
                .doesNotContain("SCAN_RECOVERY_FAILED");
    }

    static class RecoveryApplication {
    }

    @Test
    void operationalLogContainsOnlyScanIdAndCodeWithoutThrowable() {
        var logger = (Logger) LoggerFactory.getLogger(ScanExecutionGate.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var lifecycle = mock(ScanLifecyclePort.class);
            when(lifecycle.claim(id)).thenReturn(Optional.of(pending.start(pending.createdAt())));
            when(lifecycle.fail(id, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED))
                    .thenThrow(new IllegalStateException("MARCADOR_SQL_PRIVADO"));
            new ProcessScanUseCase(lifecycle, scanId -> {
                throw new IllegalStateException("MARCADOR_TRABAJO_PRIVADO");
            }, new ScanExecutionGate()).execute(id);
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getFormattedMessage())
                    .isEqualTo("scanId=" + id + " code=SCAN_EXECUTION_UNAVAILABLE");
            assertThat(appender.list.getFirst().getThrowableProxy()).isNull();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private void assertUnavailable(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(ScanOperationException.class,
                error -> assertThat(error.reason()).isEqualTo(ScanOperationException.Reason.SCAN_EXECUTION_UNAVAILABLE));
    }
}
