package com.llmscannerseguridad.integration;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import com.llmscannerseguridad.adapters.in.rest.project.*;
import com.llmscannerseguridad.adapters.in.rest.target.TargetRequestMapper;
import com.llmscannerseguridad.application.evaluation.EphemeralEvaluationCorpus;
import com.llmscannerseguridad.application.project.*;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.application.target.*;
import com.llmscannerseguridad.config.ScanWorkerEntry;
import com.llmscannerseguridad.domain.evaluation.Verdict;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "SCAN_TEST_DB_URL", matches = "jdbc:postgresql://localhost:5432/llm_scanner_seguridad_test")
@Timeout(90)
class ProjectDeletionPostgresIntegrationTest {
    private static final List<String> CHILDREN = List.of("test_executions", "evaluation_results", "findings", "scan_scores", "security_reports");
    private static final String TARGET = """
            {"url":"https://synthetic.invalid","requestTemplate":"{\\"m\\":\\"{{PROMPT}}\\"}","responsePath":"$.text","authType":"NONE"}
            """;

    @Test void deletesEmptyTargetAndFullHistoryAndKeepsOtherProjectAndCatalog() throws Exception {
        try (var db = new ScanTestDatabase()) {
            var app = open(db, new Corpus());
            var mvc = mvc(app);
            UUID empty = project(app);
            UUID onlyTarget = project(app);
            configure(app, onlyTarget);
            var a = complete(db, app);
            var b = complete(db, app);
            var life = app.getBean(ScanLifecyclePort.class);
            var failed = life.create(a.project());
            life.claim(failed.id());
            db.jdbc().update("""
                    insert into test_executions(id,scan_id,test_case_id,target_configuration_id,target_updated_at,status)
                    select ?, ?, test_case_id,target_configuration_id,target_updated_at,'PENDING'
                    from test_executions where scan_id=? limit 1
                    """, UUID.randomUUID(), failed.id(), a.scan());
            assertThat(life.fail(failed.id(), ScanStatus.RUNNING,
                    com.llmscannerseguridad.domain.scan.ScanFailureReason.SCAN_PROCESSING_FAILED)).isTrue();
            var historic = UUID.randomUUID();
            db.jdbc().update("""
                    insert into scans(id,project_id,status,created_at,started_at,finished_at)
                    values(?,?,'COMPLETED',current_timestamp,current_timestamp,current_timestamp)
                    """, historic, a.project());
            var snapshotB = snapshot(db, b.project());
            var catalog = db.jdbc().queryForList("select id from test_cases order by id");
            assertThat(catalog).hasSize(4);
            var graph = db.jdbc().queryForList("""
                    select child.relname || '>' || parent.relname from pg_constraint c
                    join pg_class child on child.oid=c.conrelid join pg_class parent on parent.oid=c.confrelid
                    join pg_namespace ns on ns.oid=child.relnamespace
                    where ns.nspname=current_schema() and c.contype='f' and c.confdeltype='a'
                    """, String.class);
            assertThat(graph).contains("target_configurations>projects", "scans>projects", "test_executions>scans",
                    "test_executions>target_configurations", "test_executions>test_cases", "evaluation_results>test_executions",
                    "evaluation_results>scans", "findings>evaluation_results", "findings>scans",
                    "scan_scores>scans", "security_reports>scans", "security_reports>scan_scores");
            assertThat(snapshot(db, a.project()).values()).allSatisfy(rows -> assertThat(rows).isNotEmpty());
            for (UUID id : List.of(empty, onlyTarget, a.project())) {
                mvc.perform(delete(path(id))).andExpect(status().isNoContent()).andExpect(content().string(""));
                mvc.perform(delete(path(id))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
                mvc.perform(get(path(id))).andExpect(status().isNotFound());
                assertThat(snapshot(db, id).values()).allSatisfy(rows -> assertThat(rows).isEmpty());
            }
            for (String table : CHILDREN) {
                assertThat(db.jdbc().queryForObject("select count(*) from " + table + " where scan_id in (?, ?, ?)",
                        Integer.class, a.scan(), historic, failed.id())).isZero();
            }
            assertThat(snapshot(db, b.project())).isEqualTo(snapshotB);
            assertThat(db.jdbc().queryForList("select id from test_cases order by id")).isEqualTo(catalog);
            mvc.perform(get("/api/projects")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(b.project().toString()));
            mvc.perform(get(path(b.project()))).andExpect(status().isOk());
        }
    }

    @Test void rollsBackIntermediateFailureAndCrossProjectReferenceAndOnlyReleasesCorpusAfterCommit() throws Exception {
        try (var db = new ScanTestDatabase()) {
            var corpus = new Corpus();
            var app = open(db, corpus);
            var a = complete(db, app);
            var b = complete(db, app);
            UUID aEvaluation = db.jdbc().queryForList("select id from evaluation_results where scan_id=?", UUID.class, a.scan()).getFirst();
            UUID bEvaluation = db.jdbc().queryForList("select id from evaluation_results where scan_id=?", UUID.class, b.scan()).getFirst();
            corpus.ids.addAll(List.of(aEvaluation, bEvaluation));
            var beforeA = snapshot(db, a.project());
            var beforeB = snapshot(db, b.project());
            db.jdbc().execute("""
                    create function fixture_delete_failure() returns trigger language plpgsql as $$
                    begin raise exception 'fixture abort'; end;
                    $$
                    """);
            db.jdbc().execute("""
                    create trigger fixture_delete_failure before delete on evaluation_results
                    for each row execute function fixture_delete_failure()
                    """);
            var mvc = mvc(app);
            mvc.perform(delete(path(a.project()))).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
            assertThat(snapshot(db, a.project())).isEqualTo(beforeA);
            assertThat(snapshot(db, b.project())).isEqualTo(beforeB);
            assertThat(corpus.released).isEmpty();
            db.jdbc().execute("drop trigger fixture_delete_failure on evaluation_results");
            UUID aTarget = db.jdbc().queryForObject("select id from target_configurations where project_id=?", UUID.class, a.project());
            UUID bTarget = db.jdbc().queryForObject("select id from target_configurations where project_id=?", UUID.class, b.project());
            db.jdbc().update("update test_executions set target_configuration_id=? where scan_id=?", aTarget, b.scan());
            var cross = snapshot(db, b.project());
            mvc.perform(delete(path(a.project()))).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PROJECT_DELETE_CONFLICT"));
            assertThat(snapshot(db, a.project())).isEqualTo(beforeA);
            assertThat(snapshot(db, b.project())).isEqualTo(cross);
            assertThat(corpus.released).isEmpty();
            db.jdbc().update("update test_executions set target_configuration_id=? where scan_id=?", bTarget, b.scan());
            corpus.onRelease = () -> {
                assertThat(snapshot(db, a.project()).values()).allSatisfy(rows -> assertThat(rows).isEmpty());
                throw new IllegalStateException("fixture cleanup failure after commit");
            };
            mvc.perform(delete(path(a.project()))).andExpect(status().isNoContent());
            assertThat(corpus.released).containsExactly(aEvaluation);
            assertThat(corpus.ids).contains(bEvaluation);
            assertThat(snapshot(db, b.project())).isEqualTo(beforeB);
        }
    }

    @Test void pendingAndRunningBlockDeletionUntilCompletionAndLateDeliveryDoesNoWork() throws Exception {
        try (var db = new ScanTestDatabase()) {
            var app = open(db, new Corpus());
            var mvc = mvc(app);
            UUID id = project(app);
            configure(app, id);
            var life = app.getBean(ScanLifecyclePort.class);
            var scan = life.create(id);
            for (ScanStatus state : List.of(ScanStatus.PENDING, ScanStatus.RUNNING)) {
                if (state == ScanStatus.RUNNING) life.claim(scan.id());
                var before = snapshot(db, id);
                mvc.perform(delete(path(id))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SCAN_ALREADY_ACTIVE"));
                assertThat(snapshot(db, id)).isEqualTo(before);
            }
            assertThat(life.fail(scan.id(), ScanStatus.RUNNING,
                    com.llmscannerseguridad.domain.scan.ScanFailureReason.SCAN_PROCESSING_FAILED)).isTrue();
            mvc.perform(delete(path(id))).andExpect(status().isNoContent());
            var work = org.mockito.Mockito.mock(ScanWorkPort.class);
            new ProcessScanUseCase(life, work, ignored -> fail("La entrega tardía no debe fallar")).execute(scan.id());
            org.mockito.Mockito.verifyNoInteractions(work);
        }
    }

    @Test void deleteWinsAgainstConcurrentDeleteAdmissionAndTargetWrites() throws Exception {
        try (var db = new ScanTestDatabase(); var workers = Executors.newFixedThreadPool(5)) {
            var corpus = new Corpus();
            var app = open(db, corpus);
            UUID project = project(app);
            configure(app, project);
            var started = new CompletableFuture<Integer>();
            var commit = new CountDownLatch(1);
            var appJdbc = app.getBean(JdbcTemplate.class);
            corpus.beforeRead = () -> {
                started.complete(appJdbc.queryForObject("select pg_backend_pid()", Integer.class));
                await(commit);
            };
            var delete = app.getBean(DeleteProjectUseCase.class);
            var dispatcher = org.mockito.Mockito.mock(ScanDispatcherPort.class);
            var start = new StartScanUseCase(app.getBean(ScanLifecyclePort.class), dispatcher, ignored -> fail("No se esperaba compensación"));
            Future<?> winner = workers.submit(() -> delete.execute(project));
            try {
                int blocker = started.get(5, TimeUnit.SECONDS);
                var duplicate = workers.submit(() -> outcome(() -> delete.execute(project)));
                var admission = workers.submit(() -> outcome(() -> start.execute(project)));
                var update = workers.submit(() -> outcome(() -> app.getBean(UpdateTargetConfigurationUseCase.class)
                        .execute(project, new TargetRequestMapper().command(TARGET))));
                var configure = workers.submit(() -> outcome(() -> configure(app, project)));
                waitForBlocked(db, blocker, 4);
                assertThat(snapshot(db, project).get("projects")).hasSize(1);
                commit.countDown();
                winner.get(8, TimeUnit.SECONDS);
                assertThat(duplicate.get(8, TimeUnit.SECONDS)).isInstanceOf(ProjectNotFoundException.class);
                assertThat(admission.get(8, TimeUnit.SECONDS)).isInstanceOfSatisfying(ScanOperationException.class,
                        error -> assertThat(error.reason()).isEqualTo(ScanOperationException.Reason.PROJECT_NOT_FOUND));
                assertThat(update.get(8, TimeUnit.SECONDS)).isInstanceOf(ProjectNotFoundException.class);
                assertThat(configure.get(8, TimeUnit.SECONDS)).isInstanceOf(ProjectNotFoundException.class);
                org.mockito.Mockito.verifyNoInteractions(dispatcher);
                assertThat(snapshot(db, project).values()).allSatisfy(rows -> assertThat(rows).isEmpty());
            } finally {
                commit.countDown();
            }
        }
    }

    @Test void deleteSeesAdmissionAndTargetWrittenWhileWaitingForRootLock() throws Exception {
        try (var db = new ScanTestDatabase(); var workers = Executors.newFixedThreadPool(2)) {
            var app = open(db, new Corpus());
            var delete = app.getBean(DeleteProjectUseCase.class);
            for (boolean admission : List.of(true, false)) {
                UUID project = project(app);
                if (admission) configure(app, project);
                var locked = new CompletableFuture<Integer>();
                var commit = new CountDownLatch(1);
                var jdbc = app.getBean(JdbcTemplate.class);
                var tx = new TransactionTemplate(app.getBean(PlatformTransactionManager.class));
                var write = workers.submit(() -> tx.executeWithoutResult(status -> {
                    if (admission) {
                        new ScanLifecycleService(app.getBean(ProjectRepositoryPort.class), app.getBean(TargetConfigurationRepositoryPort.class),
                                app.getBean(ScanRepositoryPort.class), Clock.systemUTC(), UUID::randomUUID).create(project);
                    } else {
                        app.getBean(TargetConfigurationService.class).configure(project, new TargetRequestMapper().command(TARGET));
                    }
                    locked.complete(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                    await(commit);
                }));
                try {
                    int blocker = locked.get(5, TimeUnit.SECONDS);
                    var result = workers.submit(() -> outcome(() -> delete.execute(project)));
                    waitForBlocked(db, blocker, 1);
                    commit.countDown();
                    write.get(8, TimeUnit.SECONDS);
                    var error = result.get(8, TimeUnit.SECONDS);
                    if (admission) {
                        assertThat(error).isInstanceOfSatisfying(ProjectDeletionException.class,
                                conflict -> assertThat(conflict.reason()).isEqualTo(ProjectDeletionException.Reason.SCAN_ALREADY_ACTIVE));
                        assertThat(snapshot(db, project).get("scans")).hasSize(1);
                    } else {
                        assertThat(error).isNull();
                        assertThat(snapshot(db, project).values()).allSatisfy(rows -> assertThat(rows).isEmpty());
                    }
                } finally {
                    commit.countDown();
                }
            }
        }
    }

    @Test void blocksWhileFindingsAndTerminalArtifactsAreBeingWritten() throws Exception {
        try (var db = new ScanTestDatabase(); var worker = Executors.newSingleThreadExecutor(); var barrier = db.connection()) {
            var app = open(db, new Corpus());
            var ids = ScoringDatabaseFixture.seed(db, true, Verdict.VULNERABLE);
            db.jdbc().update("update scans set report_version='report-v1' where id=?", ids.scan());
            long key = Math.abs(ids.scan().getMostSignificantBits() / 2);
            db.jdbc().execute("""
                    create function fixture_pause_write() returns trigger language plpgsql as $$
                    begin perform pg_advisory_xact_lock(TG_ARGV[0]::bigint); return NEW; end;
                    $$
                    """);
            db.jdbc().execute("create trigger pause_finding before update on findings "
                    + "for each row execute function fixture_pause_write('" + key + "')");
            db.jdbc().execute("create trigger pause_report before insert on security_reports "
                    + "for each row execute function fixture_pause_write('" + key + "')");
            int pid;
            try (var statement = barrier.createStatement(); var result = statement.executeQuery("select pg_backend_pid()")) {
                result.next();
                pid = result.getInt(1);
            }
            for (boolean completing : List.of(false, true)) {
                var before = snapshot(db, ids.project());
                try (var statement = barrier.createStatement()) {
                    statement.execute("select pg_advisory_lock(" + key + ")");
                    var write = worker.submit(() -> {
                        if (completing) assertThat(app.getBean(ScanLifecyclePort.class).complete(ids.scan())).isTrue();
                        else db.jdbc().update("update findings set evidence='Evidencia confirmada' where scan_id=?", ids.scan());
                    });
                    try {
                        waitForBlocked(db, pid, 1);
                        mvc(app).perform(delete(path(ids.project()))).andExpect(status().isConflict())
                                .andExpect(jsonPath("$.code").value("SCAN_ALREADY_ACTIVE"));
                        assertThat(snapshot(db, ids.project())).isEqualTo(before);
                    } finally {
                        statement.execute("select pg_advisory_unlock(" + key + ")");
                    }
                    write.get(8, TimeUnit.SECONDS);
                }
            }
            assertThat(snapshot(db, ids.project()).get("security_reports")).hasSize(1);
            mvc(app).perform(delete(path(ids.project()))).andExpect(status().isNoContent());
            assertThat(snapshot(db, ids.project()).values()).allSatisfy(rows -> assertThat(rows).isEmpty());
        }
    }

    private static ConfigurableApplicationContext open(ScanTestDatabase db, Corpus corpus) {
        return db.openSecurity(Map.of("logging.level.root", "ERROR"), app ->
                app.registerBean("fixtureCorpus", EphemeralEvaluationCorpus.class, () -> corpus, bean -> bean.setPrimary(true)));
    }

    private static org.springframework.test.web.servlet.MockMvc mvc(ConfigurableApplicationContext app) {
        return MockMvcBuilders.standaloneSetup(app.getBean(ProjectController.class))
                .setControllerAdvice(new ProjectExceptionHandler()).build();
    }

    private static String path(UUID project) { return "/api/projects/" + project; }
    private static UUID project(ConfigurableApplicationContext app) {
        return app.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("Fixture de borrado", null)).id();
    }

    private static void configure(ConfigurableApplicationContext app, UUID project) {
        app.getBean(ConfigureTargetUseCase.class).execute(project, new TargetRequestMapper().command(TARGET));
    }

    private static ScoringDatabaseFixture.Ids complete(ScanTestDatabase db, ConfigurableApplicationContext app) {
        var ids = ScoringDatabaseFixture.seed(db, true, Verdict.VULNERABLE);
        db.jdbc().update("update scans set report_version='report-v1' where id=?", ids.scan());
        assertThat(app.getBean(ScanLifecyclePort.class).complete(ids.scan())).isTrue();
        return ids;
    }

    private static Map<String, List<String>> snapshot(ScanTestDatabase db, UUID project) {
        var jdbc = db.jdbc();
        var result = new LinkedHashMap<String, List<String>>();
        for (String table : List.of("projects", "target_configurations", "scans")) {
            result.put(table, jdbc.queryForList("select to_jsonb(t)::text from " + table + " t where "
                    + (table.equals("projects") ? "id" : "project_id") + " = ? order by 1", String.class, project));
        }
        for (String table : CHILDREN) {
            result.put(table, jdbc.queryForList("select to_jsonb(t)::text from " + table
                    + " t where scan_id in (select id from scans where project_id=?) order by 1", String.class, project));
        }
        return result;
    }

    private static RuntimeException outcome(Runnable action) {
        try { action.run(); return null; }
        catch (RuntimeException error) { return error; }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(8, TimeUnit.SECONDS)) throw new IllegalStateException("Barrera de prueba vencida");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Barrera interrumpida", error);
        }
    }

    private static void waitForBlocked(ScanTestDatabase db, int blocker, int expected) {
        long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        do {
            int count = db.jdbc().queryForObject("""
                    with recursive blocked(pid) as (
                        select ?::integer
                        union
                        select a.pid from pg_stat_activity a join blocked b on b.pid = any(pg_blocking_pids(a.pid))
                        where a.datname=current_database()
                    ) select count(*) - 1 from blocked
                    """, Integer.class, blocker);
            if (count >= expected) return;
        } while (System.nanoTime() < limit);
        fail("Las transacciones no esperaron el lock de Project");
    }

    private static final class Corpus implements EphemeralEvaluationCorpus {
        final Set<UUID> ids = ConcurrentHashMap.newKeySet();
        final List<UUID> released = new CopyOnWriteArrayList<>();
        Runnable beforeRead = () -> {};
        Runnable onRelease = () -> {};
        @Override public Set<UUID> retainedIds() { beforeRead.run(); return Set.copyOf(ids); }
        @Override public void release(UUID id) { ids.remove(id); released.add(id); onRelease.run(); }
    }
}
