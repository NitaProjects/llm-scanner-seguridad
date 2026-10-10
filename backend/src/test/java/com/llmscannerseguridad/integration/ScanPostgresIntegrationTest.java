package com.llmscannerseguridad.integration;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.llmscannerseguridad.adapters.in.rest.scan.ScanController;
import com.llmscannerseguridad.adapters.in.rest.scan.ScanExceptionHandler;
import com.llmscannerseguridad.adapters.out.execution.scan.NoOpScanWorkAdapter;
import com.llmscannerseguridad.application.project.CreateProjectCommand;
import com.llmscannerseguridad.application.project.CreateProjectUseCase;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.scan.GetScanUseCase;
import com.llmscannerseguridad.application.scan.ListProjectScansUseCase;
import com.llmscannerseguridad.application.scan.ProcessScanUseCase;
import com.llmscannerseguridad.application.scan.ScanLifecyclePort;
import com.llmscannerseguridad.application.scan.ScanLifecycleService;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.application.scan.ScanRepositoryPort;
import com.llmscannerseguridad.application.scan.ScanWorkPort;
import com.llmscannerseguridad.application.scan.StartScanUseCase;
import com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort;
import com.llmscannerseguridad.config.ScanExecutionGate;
import com.llmscannerseguridad.config.ScanWorkerEntry;
import com.llmscannerseguridad.config.TransactionalScanLifecycle;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanFailureReason;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SE-01 a SE-10: no ejecutada por defecto; PostgreSQL real, conexiones y transacciones independientes.
@EnabledIfEnvironmentVariable(named = "SCAN_TEST_DB_URL", matches = "jdbc:postgresql:.+")
@Timeout(60)
class ScanPostgresIntegrationTest {
    private final Instant now = Instant.parse("2026-10-10T08:00:00.123456Z");

    @Test
    void appliedRunningCommitWithLostAcknowledgementBlocksWithoutOverwritingDurableClaim() throws Exception {
        try (var database = new ScanTestDatabase()) {
            var context = database.open();
            UUID project = preparedProject(database, context);
            UUID otherProject = preparedProject(database, context);
            var normal = context.getBean(ScanLifecyclePort.class);
            var pending = normal.create(project);
            var queued = normal.create(otherProject);
            var manager = context.getBean(PlatformTransactionManager.class);
            var lostAcknowledgement = mock(PlatformTransactionManager.class, delegatesTo(manager));
            var commits = new AtomicInteger();
            doAnswer(invocation -> {
                manager.commit(invocation.getArgument(0));
                if (commits.incrementAndGet() == 1) {
                    // PostgreSQL ya confirmó T2; se simula la pérdida de confirmación JDBC.
                    assertThat(database.jdbc().queryForObject("SELECT status FROM scans WHERE id = ?",
                            String.class, pending.id())).isEqualTo("RUNNING");
                    throw new TransactionSystemException("MARCADOR_CONFIRMACION_PERDIDA");
                }
                return null;
            }).when(lostAcknowledgement).commit(any());
            var gate = context.getBean(ScanExecutionGate.class);
            var uncertain = new TransactionalScanLifecycle(new ScanLifecycleService(
                    context.getBean(ProjectRepositoryPort.class), context.getBean(TargetConfigurationRepositoryPort.class),
                    context.getBean(ScanRepositoryPort.class), Clock.systemUTC(), UUID::randomUUID),
                    lostAcknowledgement, gate);
            var work = new AtomicInteger();
            new ProcessScanUseCase(uncertain, id -> work.incrementAndGet(), gate).execute(pending.id());
            assertThat(work).hasValue(0);
            var durable = context.getBean(GetScanUseCase.class).execute(project, pending.id());
            assertThat(durable.status()).isEqualTo(ScanStatus.RUNNING);
            assertThat(durable.startedAt()).isNotNull();
            assertThat(durable.finishedAt()).isNull();
            assertThat(durable.errorReason()).isNull();
            new ProcessScanUseCase(normal, id -> work.incrementAndGet(), gate).execute(queued.id());
            assertThat(work).hasValue(0);
            var mvc = mvc(context);
            mvc.perform(post("/api/projects/" + otherProject + "/scans"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("SCAN_EXECUTION_UNAVAILABLE"));
            mvc.perform(get("/api/projects/" + project + "/scans/" + pending.id()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RUNNING"));
            assertThat(context.getBean(GetScanUseCase.class).execute(project, pending.id())).isEqualTo(durable);
            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM scans", Long.class)).isEqualTo(2L);
            database.closeApplication(context);
            var restarted = database.open();
            assertThat(restarted.getBean(GetScanUseCase.class).execute(project, pending.id()).errorReason())
                    .isEqualTo(ScanFailureReason.SCAN_INTERRUPTED);
            assertThat(restarted.getBean(GetScanUseCase.class).execute(otherProject, queued.id()).errorReason())
                    .isEqualTo(ScanFailureReason.SCAN_INTERRUPTED);
            assertNoSecurityResults(database);
        }
    }

    @Test
    void pendingIsCommittedBeforeDispatchEvenWithAmbientRollbackAndWorkUsesIndependentTransactions() throws Exception {
        try (var database = new ScanTestDatabase()) {
            var context = database.open();
            UUID project = preparedProject(database, context);
            var manager = context.getBean(PlatformTransactionManager.class);
            var lifecycle = context.getBean(ScanLifecyclePort.class);
            var gate = context.getBean(ScanExecutionGate.class);
            var dispatches = new AtomicInteger();
            var start = new StartScanUseCase(lifecycle, id -> {
                // Otra conexión observa T1 antes de dispatch y antes del rollback ambiente.
                assertThat(database.jdbc().queryForObject("SELECT status FROM scans WHERE id = ?", String.class, id)).isEqualTo("PENDING");
                dispatches.incrementAndGet();
            }, gate);
            Scan pending = new TransactionTemplate(manager).execute(status -> {
                var initial = start.execute(project);
                status.setRollbackOnly();
                return initial;
            });
            assertThat(dispatches).hasValue(1);
            assertThat(context.getBean(GetScanUseCase.class).execute(project, pending.id())).isEqualTo(pending);
            var work = new AtomicInteger();
            var processor = new ProcessScanUseCase(lifecycle, id -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                assertThat(database.jdbc().queryForObject("SELECT status FROM scans WHERE id = ?", String.class, id)).isEqualTo("RUNNING");
                assertThat(context.getBean(ScanWorkPort.class)).isInstanceOf(NoOpScanWorkAdapter.class);
                context.getBean(ScanWorkPort.class).execute(id);
                work.incrementAndGet();
            }, gate);
            var entry = new ScanWorkerEntry(processor, manager, gate);
            new TransactionTemplate(manager).executeWithoutResult(status -> {
                entry.execute(pending.id());
                status.setRollbackOnly();
            });
            var completed = context.getBean(GetScanUseCase.class).execute(project, pending.id());
            assertThat(completed.status()).isEqualTo(ScanStatus.COMPLETED);
            assertThat(completed.startedAt()).isAfterOrEqualTo(completed.createdAt());
            assertThat(completed.finishedAt()).isAfterOrEqualTo(completed.startedAt());
            assertThat(completed.createdAt().getNano() % 1000).isZero();
            assertThat(work).hasValue(1);
            entry.execute(pending.id());
            assertThat(work).hasValue(1);
            assertNoSecurityResults(database);
            assertThat(database.jdbc().queryForObject("SELECT global_score FROM scans WHERE id = ?", BigDecimal.class, pending.id())).isNull();
            assertThat(lifecycle.create(project).id()).isNotEqualTo(pending.id());
        }
    }

    @Test
    void restReturns202WithoutWaitingAndPollingSeesRunningThenCompleted() throws Exception {
        var running = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var database = new ScanTestDatabase()) {
            var context = database.open(id -> {
                running.countDown();
                await(release);
                new NoOpScanWorkAdapter().execute(id);
            });
            UUID project = preparedProject(database, context);
            var mvc = mvc(context);
            String path = "/api/projects/" + project + "/scans";
            try {
                var response = mvc.perform(post(path)).andExpect(status().isAccepted())
                        .andExpect(jsonPath("$", aMapWithSize(7))).andExpect(jsonPath("$.status").value("PENDING")).andReturn();
                String location = response.getResponse().getHeader("Location");
                assertThat(location).startsWith(path + "/");
                assertThat(running.await(5, TimeUnit.SECONDS)).isTrue();
                mvc.perform(get(location)).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                        .andExpect(jsonPath("$.status").value("RUNNING"));
                mvc.perform(post(path)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SCAN_ALREADY_ACTIVE"));
                release.countDown();
                context.getBean("scanExecutor", ThreadPoolTaskExecutor.class).submit(() -> { }).get(5, TimeUnit.SECONDS);
                mvc.perform(get(location)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
                mvc.perform(get(path)).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                        .andExpect(jsonPath("$", hasSize(1)));
                assertNoSecurityResults(database);
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void concurrentStartsThatBothPassExistsAreStillExcludedByPartialUniqueIndex() throws Exception {
        try (var database = new ScanTestDatabase()) {
            var context = database.open();
            UUID project = preparedProject(database, context);
            var repository = context.getBean(ScanRepositoryPort.class);
            var barrier = new CyclicBarrier(2);
            var forcedRace = new ForwardingRepository(repository) {
                @Override
                public boolean existsActiveByProjectId(UUID projectId) {
                    boolean active = super.existsActiveByProjectId(projectId);
                    await(barrier);
                    return active;
                }
            };
            var lifecycle = lifecycle(context, forcedRace);
            var dispatches = new AtomicInteger();
            var start = new StartScanUseCase(lifecycle, id -> dispatches.incrementAndGet(), context.getBean(ScanExecutionGate.class));
            try (var callers = Executors.newFixedThreadPool(2)) {
                var first = callers.submit(() -> startOutcome(start, project));
                var second = callers.submit(() -> startOutcome(start, project));
                assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                        .containsExactlyInAnyOrder("PENDING", "SCAN_ALREADY_ACTIVE");
            }
            assertThat(dispatches).hasValue(1);
            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM scans", Long.class)).isEqualTo(1L);
            var winner = repository.findByProjectIdOrdered(project).getFirst();
            assertThat(lifecycle.fail(winner.id(), ScanStatus.PENDING, ScanFailureReason.SCAN_PROCESSING_FAILED)).isTrue();
            // Un terminal libera el mismo Project; otro Project admite un activo independiente.
            var next = context.getBean(ScanLifecyclePort.class).create(project);
            var other = context.getBean(ScanLifecyclePort.class).create(preparedProject(database, context));
            assertThat(next.id()).isNotEqualTo(winner.id());
            assertThat(other.projectId()).isNotEqualTo(project);
            assertThat(repository.existsActiveByProjectId(project)).isTrue();
        }
    }

    @Test
    void twoConcurrentPendingClaimsCommitOnlyOneWorker() throws Exception {
        try (var database = new ScanTestDatabase()) {
            var context = database.open();
            UUID project = preparedProject(database, context);
            var pending = context.getBean(ScanLifecyclePort.class).create(project);
            var reads = new AtomicInteger();
            var barrier = new CyclicBarrier(2);
            var racedRepository = new ForwardingRepository(context.getBean(ScanRepositoryPort.class)) {
                @Override
                public Optional<Scan> findById(UUID scanId) {
                    var result = super.findById(scanId);
                    if (reads.incrementAndGet() <= 2) {
                        await(barrier);
                    }
                    return result;
                }
            };
            var gate = context.getBean(ScanExecutionGate.class);
            var work = new AtomicInteger();
            var process = new ProcessScanUseCase(lifecycle(context, racedRepository), id -> work.incrementAndGet(), gate);
            var entry = new ScanWorkerEntry(process, context.getBean(PlatformTransactionManager.class), gate);
            try (var workers = Executors.newFixedThreadPool(2)) {
                var first = workers.submit(() -> entry.execute(pending.id()));
                var second = workers.submit(() -> entry.execute(pending.id()));
                first.get(10, TimeUnit.SECONDS);
                second.get(10, TimeUnit.SECONDS);
            }
            assertThat(work).hasValue(1);
            assertThat(context.getBean(GetScanUseCase.class).execute(project, pending.id()).status()).isEqualTo(ScanStatus.COMPLETED);
        }
    }

    @Test
    void insertAndDeferredCommitFailuresRollBackWithoutAnyDispatch() throws Exception {
        for (boolean deferred : new boolean[] {false, true}) {
            try (var database = new ScanTestDatabase()) {
                var context = database.open();
                UUID project = preparedProject(database, context);
                database.jdbc().execute("""
                        CREATE FUNCTION reject_scan_insert() RETURNS trigger LANGUAGE plpgsql AS $$
                        BEGIN RAISE EXCEPTION 'MARCADOR_INTERNO_INSERT' USING ERRCODE = '23514'; END;
                        $$
                        """);
                database.jdbc().execute(deferred ? """
                        CREATE CONSTRAINT TRIGGER reject_scan_insert AFTER INSERT ON scans
                        DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION reject_scan_insert()
                        """ : "CREATE TRIGGER reject_scan_insert BEFORE INSERT ON scans FOR EACH ROW EXECUTE FUNCTION reject_scan_insert()");
                var dispatches = new AtomicInteger();
                var start = new StartScanUseCase(context.getBean(ScanLifecyclePort.class), id -> dispatches.incrementAndGet(),
                        context.getBean(ScanExecutionGate.class));
                assertThatThrownBy(() -> start.execute(project)).isInstanceOfSatisfying(ScanOperationException.class,
                        error -> assertThat(error.reason()).isEqualTo(ScanOperationException.Reason.INTERNAL_ERROR));
                assertThat(dispatches).hasValue(0);
                assertThat(database.jdbc().queryForObject("SELECT count(*) FROM scans", Long.class)).isZero();
            }
        }
    }

    @Test
    void dispatchRejectionPersistsFailedAndRestReturnsLocation() throws Exception {
        try (var database = new ScanTestDatabase()) {
            var context = database.open();
            UUID project = preparedProject(database, context);
            context.getBean("scanExecutor", ThreadPoolTaskExecutor.class).shutdown();
            String path = "/api/projects/" + project + "/scans";
            var response = mvc(context).perform(post(path)).andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("SCAN_DISPATCH_FAILED")).andReturn();
            mvc(context).perform(get(response.getResponse().getHeader("Location"))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED")).andExpect(jsonPath("$.startedAt").value((Object) null))
                    .andExpect(jsonPath("$.errorReason").value("SCAN_DISPATCH_FAILED"));
        }
    }

    @Test
    void failedDispatchCompensationReturns500WithLocationAndBlocksNewStartsWithoutInventingFailed() throws Exception {
        try (var database = new ScanTestDatabase()) {
            var context = database.open();
            UUID project = preparedProject(database, context);
            rejectUpdates(database, "NEW.status = 'FAILED'", true);
            context.getBean("scanExecutor", ThreadPoolTaskExecutor.class).shutdown();
            String path = "/api/projects/" + project + "/scans";
            var response = mvc(context).perform(post(path)).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR")).andReturn();
            String location = response.getResponse().getHeader("Location");
            assertThat(location).startsWith(path + "/");
            mvc(context).perform(get(location)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
            mvc(context).perform(post(path)).andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("SCAN_EXECUTION_UNAVAILABLE"));
            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM scans", Long.class)).isEqualTo(1L);
        }
    }

    @Test
    void processingAndTransitionFailuresUseNewFailureTransactionsAndClosedCodes() throws Exception {
        for (String failure : List.of("WORK", "RUNNING", "COMPLETED")) {
            try (var database = new ScanTestDatabase()) {
                var context = database.open();
                UUID project = preparedProject(database, context);
                var lifecycle = context.getBean(ScanLifecyclePort.class);
                var pending = lifecycle.create(project);
                if (!failure.equals("WORK")) {
                    rejectUpdates(database, "NEW.status = '" + failure + "'", true);
                }
                var work = new AtomicInteger();
                new ProcessScanUseCase(lifecycle, id -> {
                    work.incrementAndGet();
                    if (failure.equals("WORK")) {
                        throw new IllegalStateException("MARCADOR_WORKER_PRIVADO");
                    }
                }, context.getBean(ScanExecutionGate.class)).execute(pending.id());
                var failed = context.getBean(GetScanUseCase.class).execute(project, pending.id());
                assertThat(failed.status()).isEqualTo(ScanStatus.FAILED);
                assertThat(failed.errorReason()).isEqualTo(ScanFailureReason.SCAN_PROCESSING_FAILED);
                assertThat(failed.finishedAt()).isNotNull();
                if (failure.equals("RUNNING")) {
                    assertThat(work).hasValue(0);
                    assertThat(failed.startedAt()).isNull();
                } else {
                    assertThat(work).hasValue(1);
                    assertThat(failed.startedAt()).isNotNull();
                }
                assertThat(database.jdbc().queryForObject("SELECT error_reason FROM scans WHERE id = ?", String.class, pending.id()))
                        .isEqualTo("SCAN_PROCESSING_FAILED");
            }
        }
    }

    @Test
    void unrecoverableTerminalFailureBlocksStartsAndQueuedClaimsButLeavesConfirmedReads() throws Exception {
        try (var database = new ScanTestDatabase()) {
            var context = database.open();
            UUID project = preparedProject(database, context);
            UUID queuedProject = preparedProject(database, context);
            var lifecycle = context.getBean(ScanLifecyclePort.class);
            var scan = lifecycle.create(project);
            var queued = lifecycle.create(queuedProject);
            rejectUpdates(database, "NEW.status IN ('COMPLETED', 'FAILED')", true);
            context.getBean(ScanWorkerEntry.class).execute(scan.id());
            assertThat(context.getBean(GetScanUseCase.class).execute(project, scan.id()).status()).isEqualTo(ScanStatus.RUNNING);
            context.getBean(ScanWorkerEntry.class).execute(queued.id());
            assertThat(context.getBean(GetScanUseCase.class).execute(queuedProject, queued.id()).status()).isEqualTo(ScanStatus.PENDING);
            mvc(context).perform(post("/api/projects/" + preparedProject(database, context) + "/scans"))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("SCAN_EXECUTION_UNAVAILABLE"));
            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM scans", Long.class)).isEqualTo(2L);
            database.closeApplication(context);
            database.jdbc().execute("DROP TRIGGER reject_scan_update ON scans");
            var recovered = database.open();
            assertThat(recovered.getBean(GetScanUseCase.class).execute(project, scan.id()).errorReason()).isEqualTo(ScanFailureReason.SCAN_INTERRUPTED);
            assertThat(recovered.getBean(GetScanUseCase.class).execute(queuedProject, queued.id()).errorReason()).isEqualTo(ScanFailureReason.SCAN_INTERRUPTED);
        }
    }

    @Test
    void sequentialRestartRecoversActiveRowsOnlyPreservesDatesAndFreesProjects() throws Exception {
        try (var database = new ScanTestDatabase()) {
            var first = database.open();
            var lifecycle = first.getBean(ScanLifecyclePort.class);
            var pending = lifecycle.create(preparedProject(database, first));
            var running = lifecycle.create(preparedProject(database, first));
            lifecycle.claim(running.id());
            var completed = lifecycle.create(preparedProject(database, first));
            lifecycle.claim(completed.id());
            lifecycle.complete(completed.id());
            var failed = lifecycle.create(preparedProject(database, first));
            lifecycle.fail(failed.id(), ScanStatus.PENDING, ScanFailureReason.SCAN_DISPATCH_FAILED);
            var repository = first.getBean(ScanRepositoryPort.class);
            var beforeCompleted = repository.findById(completed.id()).orElseThrow();
            var beforeFailed = repository.findById(failed.id()).orElseThrow();
            var beforeRunning = repository.findById(running.id()).orElseThrow();
            // Un reloj de arranque anterior se limita a la última fecha persistida.
            database.jdbc().update("UPDATE scans SET created_at = '2100-01-01T00:00:00Z' WHERE id = ?", pending.id());
            database.closeApplication(first);
            var relaunches = new AtomicInteger();
            var second = database.open(id -> relaunches.incrementAndGet());
            second.getBean("scanExecutor", ThreadPoolTaskExecutor.class).submit(() -> { }).get(5, TimeUnit.SECONDS);
            assertThat(relaunches).hasValue(0);
            var after = second.getBean(ScanRepositoryPort.class);
            assertThat(after.findById(completed.id())).contains(beforeCompleted);
            assertThat(after.findById(failed.id())).contains(beforeFailed);
            var recoveredPending = after.findById(pending.id()).orElseThrow();
            var recoveredRunning = after.findById(running.id()).orElseThrow();
            assertThat(recoveredPending.errorReason()).isEqualTo(ScanFailureReason.SCAN_INTERRUPTED);
            assertThat(recoveredPending.startedAt()).isNull();
            assertThat(recoveredPending.finishedAt()).isEqualTo(recoveredPending.createdAt());
            assertThat(recoveredRunning.errorReason()).isEqualTo(ScanFailureReason.SCAN_INTERRUPTED);
            assertThat(recoveredRunning.startedAt()).isEqualTo(beforeRunning.startedAt());
            assertThat(after.findActive()).isEmpty();
            assertThat(second.getBean(ScanLifecyclePort.class).create(pending.projectId()).status()).isEqualTo(ScanStatus.PENDING);
            assertNoSecurityResults(database);
        }
    }

    @Test
    void failedRecoveryPreventsApplicationStartupAndRollsBackWholeRecovery() throws Exception {
        try (var database = new ScanTestDatabase()) {
            var first = database.open();
            var scan = first.getBean(ScanLifecyclePort.class).create(preparedProject(database, first));
            rejectUpdates(database, "NEW.status = 'FAILED'", true);
            database.closeApplication(first);
            assertThatThrownBy(database::open).isInstanceOf(RuntimeException.class);
            assertThat(database.jdbc().queryForObject("SELECT status FROM scans WHERE id = ?", String.class, scan.id())).isEqualTo("PENDING");
        }
    }

    @Test
    void orderedScopedQueriesPreserveLegacyScoreAndDatabaseConstraintsRejectInvalidRows() throws Exception {
        try (var database = new ScanTestDatabase()) {
            var context = database.open();
            UUID project = preparedProject(database, context);
            var lifecycle = context.getBean(ScanLifecyclePort.class);
            var pending = lifecycle.create(project);
            database.jdbc().update("UPDATE scans SET global_score = 73.25 WHERE id = ?", pending.id());
            lifecycle.claim(pending.id());
            lifecycle.complete(pending.id());
            assertThat(database.jdbc().queryForObject("SELECT global_score FROM scans WHERE id = ?", BigDecimal.class, pending.id()))
                    .isEqualByComparingTo("73.25");
            Instant tie = Instant.parse("2100-01-01T00:00:00.123456Z");
            var low = Scan.pending(UUID.fromString("00000000-0000-0000-0000-000000000001"), project, tie)
                    .start(tie).complete(tie);
            var high = Scan.pending(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), project, low.createdAt())
                    .start(low.createdAt()).complete(low.createdAt());
            var scans = context.getBean(ScanRepositoryPort.class);
            new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(status -> {
                scans.insert(high);
                scans.insert(low);
            });
            var listed = context.getBean(ListProjectScansUseCase.class).execute(project);
            // Las fechas de empate se fijan después de la fecha real de creación para comprobar el orden.
            assertThat(listed).containsExactly(low, high, scans.findById(pending.id()).orElseThrow());
            UUID other = preparedProject(database, context);
            mvc(context).perform(get("/api/projects/" + other + "/scans/" + pending.id()))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SCAN_NOT_FOUND"));
            for (String values : List.of(
                    "'UNKNOWN', NULL, NULL, NULL", "'PENDING', CURRENT_TIMESTAMP, NULL, NULL",
                    "'RUNNING', NULL, NULL, NULL", "'RUNNING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL",
                    "'COMPLETED', NULL, CURRENT_TIMESTAMP, NULL", "'FAILED', NULL, CURRENT_TIMESTAMP, NULL",
                    "'FAILED', NULL, CURRENT_TIMESTAMP, 'UNKNOWN'",
                    "'FAILED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SCAN_DISPATCH_FAILED'",
                    "'PENDING', NULL, NULL, 'SCAN_PROCESSING_FAILED'",
                    "'RUNNING', '2000-01-01T00:00:00Z', NULL, NULL",
                    "'COMPLETED', CURRENT_TIMESTAMP, '2000-01-01T00:00:00Z', NULL")) {
                assertThatThrownBy(() -> database.jdbc().update("""
                        INSERT INTO scans(id, project_id, created_at, status, started_at, finished_at, error_reason)
                        VALUES (?, ?, CURRENT_TIMESTAMP, %s)
                        """.formatted(values), UUID.randomUUID(), project)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            }
            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM scans", Long.class)).isEqualTo(3L);
            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname = ? AND indexname IN ('idx_scans_project_id', 'uq_scans_active_project')",
                    Long.class, database.schema)).isEqualTo(2L);
            assertNoSecurityResults(database);
        }
    }

    private UUID preparedProject(ScanTestDatabase database, ConfigurableApplicationContext context) {
        UUID project = context.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("Scan", null)).id();
        // Blob sintético deliberadamente opaco: iniciar/leer no debe descifrar ni necesitar clave.
        database.jdbc().update("""
                INSERT INTO target_configurations(id, project_id, url, request_template, response_path,
                    auth_type, credential_ciphertext, timeout_ms, created_at, updated_at)
                VALUES (?, ?, 'https://example.com/chat', '{"message":"{{PROMPT}}"}', '$.text',
                    'BEARER_TOKEN', decode('00010203', 'hex'), 10000, ?, ?)
                """, UUID.randomUUID(), project, Timestamp.from(now), Timestamp.from(now));
        return project;
    }

    private TransactionalScanLifecycle lifecycle(ConfigurableApplicationContext context, ScanRepositoryPort repository) {
        return new TransactionalScanLifecycle(new ScanLifecycleService(context.getBean(ProjectRepositoryPort.class),
                context.getBean(TargetConfigurationRepositoryPort.class), repository, Clock.systemUTC(), UUID::randomUUID),
                context.getBean(PlatformTransactionManager.class), context.getBean(ScanExecutionGate.class));
    }

    private String startOutcome(StartScanUseCase start, UUID project) {
        try {
            return start.execute(project).status().name();
        } catch (ScanOperationException error) {
            return error.reason().name();
        }
    }

    private MockMvc mvc(ConfigurableApplicationContext context) {
        return MockMvcBuilders.standaloneSetup(context.getBean(ScanController.class))
                .setControllerAdvice(context.getBean(ScanExceptionHandler.class)).build();
    }

    private void assertNoSecurityResults(ScanTestDatabase database) {
        for (String table : List.of("test_cases", "test_executions", "findings")) {
            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
        }
    }

    private void rejectUpdates(ScanTestDatabase database, String condition, boolean deferred) {
        database.jdbc().execute("""
                CREATE FUNCTION reject_scan_update() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'MARCADOR_INTERNO_TRANSICION' USING ERRCODE = '23514'; END;
                $$
                """);
        database.jdbc().execute((deferred ? """
                CREATE CONSTRAINT TRIGGER reject_scan_update AFTER UPDATE ON scans
                DEFERRABLE INITIALLY DEFERRED FOR EACH ROW WHEN (%s) EXECUTE FUNCTION reject_scan_update()
                """ : "CREATE TRIGGER reject_scan_update BEFORE UPDATE ON scans FOR EACH ROW WHEN (%s) EXECUTE FUNCTION reject_scan_update()")
                .formatted(condition));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Barrera de prueba no liberada.");
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba interrumpida.");
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (Exception error) {
            throw new IllegalStateException("Barrera de prueba no completada.");
        }
    }

    private static class ForwardingRepository implements ScanRepositoryPort {
        private final ScanRepositoryPort delegate;
        ForwardingRepository(ScanRepositoryPort delegate) { this.delegate = delegate; }
        public void insert(Scan scan) { delegate.insert(scan); }
        public boolean existsActiveByProjectId(UUID projectId) { return delegate.existsActiveByProjectId(projectId); }
        public Optional<Scan> findById(UUID id) { return delegate.findById(id); }
        public Optional<Scan> findByProjectIdAndId(UUID projectId, UUID id) { return delegate.findByProjectIdAndId(projectId, id); }
        public List<Scan> findByProjectIdOrdered(UUID projectId) { return delegate.findByProjectIdOrdered(projectId); }
        public List<Scan> findActive() { return delegate.findActive(); }
        public boolean transition(ScanStatus expected, Scan scan) { return delegate.transition(expected, scan); }
    }
}
