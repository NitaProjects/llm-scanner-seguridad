package com.llmscannerseguridad.integration;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.llmscannerseguridad.adapters.in.rest.scan.*;
import com.llmscannerseguridad.adapters.out.http.target.ControlledTargetServer;
import com.llmscannerseguridad.adapters.out.http.target.HttpTargetInvocationAdapter;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.application.project.*;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.application.target.CredentialEncryptionPort;
import com.llmscannerseguridad.config.ScanWorkerEntry;
import com.llmscannerseguridad.domain.execution.*;
import com.llmscannerseguridad.domain.scan.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Ejecutar únicamente contra PostgreSQL 16 dedicado y con autorización expresa.
@EnabledIfEnvironmentVariable(named = "SCAN_TEST_DB_URL", matches = "jdbc:postgresql:.+")
@Timeout(90)
class SecurityTestExecutionPostgresIntegrationTest {
    @Test
    void restWorkerHttpPostgresPollingPlanCommitAuthRedactionAndUnicodeRoundTrip() throws Exception {
        for (String auth : List.of("NONE", "BEARER_TOKEN", "API_KEY_HEADER")) {
            try (var server = new ControlledTargetServer(); var database = new ScanTestDatabase()) {
                var context = database.openSecurity(settings(server), application -> application.registerBean("observedInvocation",
                        TargetInvocationPort.class, () -> (target, prompt, budget) -> {
                            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                            // Esta lectura usa otra conexión y exige las cuatro filas antes de HTTP.
                            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM test_executions", Long.class)).isEqualTo(4L);
                            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM test_executions WHERE status = 'RUNNING'", Long.class))
                                    .isEqualTo(1L);
                            return application.getBean(HttpTargetInvocationAdapter.class).invoke(target, prompt, budget);
                        }, definition -> definition.setPrimary(true)));
                UUID project = target(database, context, server.origin() + (auth.equals("NONE") ? "/nul" : "/echo"), auth);
                var mvc = MockMvcBuilders.standaloneSetup(context.getBean(ScanController.class), context.getBean(TestExecutionController.class))
                        .setControllerAdvice(new ScanExceptionHandler()).build();
                var response = mvc.perform(post("/api/projects/" + project + "/scans")).andExpect(status().isAccepted()).andReturn();
                String location = response.getResponse().getHeader("Location");
                context.getBean("scanExecutor", ThreadPoolTaskExecutor.class).submit(() -> { }).get(20, TimeUnit.SECONDS);
                mvc.perform(get(location)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
                mvc.perform(get(location + "/executions")).andExpect(status().isOk())
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andExpect(jsonPath("$.length()").value(4)).andExpect(jsonPath("$[0].requestBody").doesNotExist());
                UUID scan = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
                var rows = context.getBean(TestExecutionRepositoryPort.class).findByScanId(scan);
                assertThat(rows).hasSize(4).allSatisfy(row -> {
                    assertThat(row.status()).isEqualTo(ExecutionStatus.COMPLETED);
                    if (!auth.equals("NONE")) assertThat(row.requestBody()).doesNotContain("fixture-secret");
                    assertThat(row.responseText()).isEqualTo(auth.equals("NONE") ? "a\0😀"
                            : auth.equals("BEARER_TOKEN") ? "Bearer [REDACTED_CREDENTIAL]" : "[REDACTED_CREDENTIAL]");
                });
                assertThat(server.received).hasSize(4);
                assertThat(database.jdbc().queryForObject(
                        "SELECT count(*) FROM test_executions WHERE response_payload LIKE '%\\u0000%'", Long.class))
                        .isEqualTo(auth.equals("NONE") ? 4L : 0L);
                assertNoEvaluation(database);
            }
        }
    }

    @Test
    void individualFailureContinuesGlobalFailureAbortsAndPreviousSuccessesSurvive() throws Exception {
        try (var server = new ControlledTargetServer(); var database = new ScanTestDatabase()) {
            var context = database.openSecurity(settings(server));
            UUID project = target(database, context, server.origin() + "/sequence", "NONE");
            UUID scan = run(context, project);
            var rows = context.getBean(TestExecutionRepositoryPort.class).findByScanId(scan);
            assertThat(server.received).hasSize(4);
            assertThat(rows.stream().filter(row -> row.status() == ExecutionStatus.COMPLETED)).hasSize(3);
            assertThat(rows.stream().filter(row -> row.errorReason() == ExecutionError.RESPONSE_INVALID_JSON)).hasSize(1);
            assertThat(context.getBean(GetScanUseCase.class).execute(project, scan).status()).isEqualTo(ScanStatus.FAILED);
            assertNoEvaluation(database);
        }
        try (var server = new ControlledTargetServer(); var database = new ScanTestDatabase()) {
            var context = database.openSecurity(Map.of());
            UUID project = target(database, context, server.origin(), "NONE");
            UUID scan = run(context, project);
            var rows = context.getBean(TestExecutionRepositoryPort.class).findByScanId(scan);
            assertThat(server.received).isEmpty();
            assertThat(rows.stream().filter(row -> row.errorReason() == ExecutionError.TARGET_NOT_AUTHORIZED)).hasSize(1);
            assertThat(rows.stream().filter(row -> row.errorReason() == ExecutionError.EXECUTION_ABORTED)).hasSize(3);
            assertThat(context.getBean(GetScanUseCase.class).execute(project, scan).status()).isEqualTo(ScanStatus.FAILED);
        }
    }

    @Test
    void preparationRollbackAndRecoveryAreAtomicAndTerminalEvidenceSurvivesRestart() throws Exception {
        try (var server = new ControlledTargetServer(); var database = new ScanTestDatabase()) {
            var settings = settings(server);
            var context = database.openSecurity(settings);
            UUID project = target(database, context, server.origin(), "NONE");
            var lifecycle = context.getBean(ScanLifecyclePort.class);
            UUID scan = lifecycle.create(project).id();
            lifecycle.claim(scan);
            var executions = context.getBean(TestExecutionRepositoryPort.class);
            var plan = executions.prepare(scan);
            var first = executions.start(plan.executions().getFirst().id());
            executions.finish(first, new InvocationResult(200, 2L, null, "{}", "😀\0", false, false));
            executions.start(plan.executions().get(1).id());
            database.closeApplication(context);
            var restarted = database.openSecurity(settings);
            var rows = restarted.getBean(TestExecutionRepositoryPort.class).findByScanId(scan);
            assertThat(rows.stream().filter(row -> row.status() == ExecutionStatus.COMPLETED)).hasSize(1)
                    .allSatisfy(row -> assertThat(row.responseText()).isEqualTo("😀\0"));
            assertThat(rows.stream().filter(row -> row.errorReason() == ExecutionError.EXECUTION_INTERRUPTED)).hasSize(3);
            assertThat(restarted.getBean(GetScanUseCase.class).execute(project, scan).errorReason()).isEqualTo(ScanFailureReason.SCAN_INTERRUPTED);
            assertThat(server.received).isEmpty();
            var newer = restarted.getBean(ScanLifecyclePort.class).create(project);
            restarted.getBean(ScanLifecyclePort.class).claim(newer.id());
            database.jdbc().execute("""
                    CREATE FUNCTION reject_execution_plan() RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN RAISE EXCEPTION 'SYNTHETIC_INSERT_FAILURE'; END $$;
                    CREATE TRIGGER reject_execution_plan BEFORE INSERT ON test_executions
                    FOR EACH ROW EXECUTE FUNCTION reject_execution_plan()
                    """);
            assertThatThrownBy(() -> restarted.getBean(TestExecutionRepositoryPort.class).prepare(newer.id())).isInstanceOf(ScanOperationException.class);
            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM test_executions WHERE scan_id = ?", Long.class, newer.id())).isZero();
        }
    }

    @Test
    void accumulatedEvidenceExhaustionStopsBeforeNextRequestAndBudgetProducesClosedError() throws Exception {
        try (var server = new ControlledTargetServer(); var database = new ScanTestDatabase()) {
            var settings = new java.util.HashMap<>(settings(server));
            settings.put("TEST_EXECUTION_EVIDENCE_BYTES", 512);
            settings.put("TEST_EXECUTION_SCAN_EVIDENCE_BYTES", 512);
            var context = database.openSecurity(settings);
            UUID project = target(database, context, server.origin(), "NONE");
            UUID scan = run(context, project);
            assertThat(context.getBean(GetScanUseCase.class).execute(project, scan).status()).isEqualTo(ScanStatus.FAILED);
            assertThat(server.received).hasSize(2);
            var rows = context.getBean(TestExecutionRepositoryPort.class).findByScanId(scan);
            assertThat(rows)
                    .anySatisfy(row -> assertThat(row.errorReason()).isEqualTo(ExecutionError.EVIDENCE_LIMIT_EXCEEDED));
            assertThat(rows.stream().filter(row -> row.status() == ExecutionStatus.COMPLETED)).hasSize(2)
                    .allSatisfy(row -> {
                        assertThat(row.requestBody()).isNotNull();
                        assertThat(row.responseText()).isEqualTo("ok");
                    });
            assertThat(rows.stream().filter(row -> row.errorReason() == ExecutionError.EVIDENCE_LIMIT_EXCEEDED)).hasSize(1)
                    .allSatisfy(row -> {
                        assertThat(row.status()).isEqualTo(ExecutionStatus.FAILED);
                        assertThat(row.startedAt()).isNotNull();
                        assertThat(row.finishedAt()).isNotNull();
                        assertThat(row.httpStatus()).isNull();
                        assertThat(row.latencyMs()).isNull();
                        assertThat(row.requestBody()).isNull();
                        assertThat(row.responseText()).isNull();
                    });
            assertThat(rows.stream().filter(row -> row.errorReason() == ExecutionError.EXECUTION_ABORTED)).hasSize(1)
                    .allSatisfy(row -> {
                        assertThat(row.status()).isEqualTo(ExecutionStatus.FAILED);
                        assertThat(row.startedAt()).isNull();
                    });
            assertThat(database.jdbc().queryForObject("""
                    SELECT sum(octet_length(coalesce(request_payload,'')) + octet_length(coalesce(response_payload,'')))
                    FROM test_executions WHERE scan_id = ?
                    """, Long.class, scan)).isLessThanOrEqualTo(512L);
        }
        try (var server = new ControlledTargetServer(); var database = new ScanTestDatabase()) {
            var context = database.openSecurity(settings(server));
            UUID project = target(database, context, server.origin(), "NONE");
            var lifecycle = context.getBean(ScanLifecyclePort.class);
            UUID scan = lifecycle.create(project).id();
            lifecycle.claim(scan);
            database.jdbc().update("UPDATE scans SET created_at = CURRENT_TIMESTAMP - interval '302 seconds', started_at = CURRENT_TIMESTAMP - interval '301 seconds' WHERE id = ?", scan);
            assertThatThrownBy(() -> context.getBean(ScanWorkPort.class).execute(scan)).isInstanceOf(ScanOperationException.class);
            lifecycle.fail(scan, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED);
            assertThat(server.received).isEmpty();
            assertThat(context.getBean(TestExecutionRepositoryPort.class).findByScanId(scan))
                    .anySatisfy(row -> assertThat(row.errorReason()).isEqualTo(ExecutionError.SCAN_BUDGET_EXCEEDED));
        }
    }

    private Map<String, Object> settings(ControlledTargetServer server) {
        return Map.of("TARGET_LOCAL_DEMO_ENABLED", "true", "TARGET_LOCAL_DEMO_ORIGINS", server.origin(),
                "TARGET_CREDENTIAL_ENCRYPTION_KEY", Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void scanTerminalizationAndRecoveryRollBackChildrenTogetherAndDoNotReopenTerminalScan() throws Exception {
        try (var server = new ControlledTargetServer(); var database = new ScanTestDatabase()) {
            var context = database.openSecurity(settings(server));
            UUID project = target(database, context, server.origin(), "NONE");
            var lifecycle = context.getBean(ScanLifecyclePort.class);
            UUID scan = lifecycle.create(project).id();
            lifecycle.claim(scan);
            var repository = context.getBean(TestExecutionRepositoryPort.class);
            repository.prepare(scan);
            database.jdbc().execute("""
                    CREATE FUNCTION reject_scan_terminal() RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN IF NEW.status = 'FAILED' THEN RAISE EXCEPTION 'SYNTHETIC_TERMINAL_FAILURE'; END IF;
                    RETURN NEW; END $$;
                    CREATE TRIGGER reject_scan_terminal BEFORE UPDATE ON scans
                    FOR EACH ROW EXECUTE FUNCTION reject_scan_terminal()
                    """);
            assertThatThrownBy(() -> lifecycle.fail(scan, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED))
                    .isInstanceOf(ScanOperationException.class);
            assertThat(repository.findByScanId(scan)).allSatisfy(row -> assertThat(row.status()).isEqualTo(ExecutionStatus.PENDING));
            assertThatThrownBy(lifecycle::recoverInterrupted).isInstanceOf(ScanOperationException.class);
            assertThat(repository.findByScanId(scan)).allSatisfy(row -> assertThat(row.status()).isEqualTo(ExecutionStatus.PENDING));
            assertThat(context.getBean(GetScanUseCase.class).execute(project, scan).status()).isEqualTo(ScanStatus.RUNNING);
            database.jdbc().execute("DROP TRIGGER reject_scan_terminal ON scans");
            // Simula un hijo activo heredado de Scan terminal, sin reescribir después ese Scan.
            database.jdbc().update("UPDATE scans SET status='COMPLETED', finished_at=started_at WHERE id=?", scan);
            assertThat(lifecycle.fail(scan, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED)).isFalse();
            assertThat(repository.findByScanId(scan)).allSatisfy(row -> assertThat(row.status()).isEqualTo(ExecutionStatus.PENDING));
            lifecycle.recoverInterrupted();
            assertThat(repository.findByScanId(scan)).allSatisfy(row -> assertThat(row.errorReason()).isEqualTo(ExecutionError.EXECUTION_INTERRUPTED));
            assertThat(context.getBean(GetScanUseCase.class).execute(project, scan).status()).isEqualTo(ScanStatus.COMPLETED);
        }
    }

    @Test
    void catalogAndTargetAreFixedForScanAndNextScanReadsUpdatedConfiguration() throws Exception {
        try (var server = new ControlledTargetServer(); var database = new ScanTestDatabase()) {
            var calls = new java.util.concurrent.atomic.AtomicInteger();
            var context = database.openSecurity(settings(server), application -> application.registerBean("changingTargetInvocation",
                    TargetInvocationPort.class, () -> (target, prompt, budget) -> {
                        var result = application.getBean(HttpTargetInvocationAdapter.class).invoke(target, prompt, budget);
                        if (calls.incrementAndGet() == 1) database.jdbc().update(
                                "UPDATE target_configurations SET url='https://unapproved.invalid', updated_at=updated_at + interval '1 second' WHERE id=?", target.id());
                        return result;
                    }, definition -> definition.setPrimary(true)));
            UUID project = target(database, context, server.origin(), "NONE");
            UUID first = run(context, project);
            assertThat(context.getBean(GetScanUseCase.class).execute(project, first).status()).isEqualTo(ScanStatus.COMPLETED);
            assertThat(server.received).hasSize(4);
            UUID second = run(context, project);
            assertThat(context.getBean(GetScanUseCase.class).execute(project, second).status()).isEqualTo(ScanStatus.FAILED);
            assertThat(server.received).hasSize(4);
            assertThat(context.getBean(TestExecutionRepositoryPort.class).findByScanId(first).stream().map(TestExecution::targetUpdatedAt).distinct()).hasSize(1);
        }
    }

    @Test
    void uniquenessChecksConditionalTransitionsAndDeferredPlanCommitAreRealPostgresGuarantees() throws Exception {
        try (var server = new ControlledTargetServer(); var database = new ScanTestDatabase()) {
            var context = database.openSecurity(settings(server));
            UUID project = target(database, context, server.origin(), "NONE");
            var lifecycle = context.getBean(ScanLifecyclePort.class);
            var repository = context.getBean(TestExecutionRepositoryPort.class);
            UUID scan = lifecycle.create(project).id();
            lifecycle.claim(scan);
            var plan = repository.prepare(scan);
            UUID execution = plan.executions().getFirst().id();
            for (String set : List.of("status='OTHER'", "status='COMPLETED'", "status='FAILED',finished_at=CURRENT_TIMESTAMP",
                    "http_status=600", "latency_ms=-1", "request_redacted=true", "response_redacted=true")) {
                assertThatThrownBy(() -> database.jdbc().update("UPDATE test_executions SET " + set + " WHERE id=?", execution))
                        .isInstanceOf(RuntimeException.class);
            }
            assertThatThrownBy(() -> database.jdbc().update("""
                    INSERT INTO test_executions(id,scan_id,test_case_id,target_configuration_id,target_updated_at,status)
                    SELECT ?,scan_id,test_case_id,target_configuration_id,target_updated_at,'PENDING'
                    FROM test_executions WHERE id=?
                    """, UUID.randomUUID(), execution)).isInstanceOf(RuntimeException.class);
            var running = repository.start(execution);
            assertThatThrownBy(() -> repository.start(execution)).isInstanceOf(ScanOperationException.class);
            repository.finish(running, new InvocationResult(200, 1L, null, "{}", "", false, false));
            assertThatThrownBy(() -> repository.finish(running, new InvocationResult(200, 1L, null, "{}", "", false, false)))
                    .isInstanceOf(ScanOperationException.class);
            lifecycle.fail(scan, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED);
            UUID next = lifecycle.create(project).id();
            lifecycle.claim(next);
            database.jdbc().execute("""
                    CREATE FUNCTION reject_deferred_plan() RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN RAISE EXCEPTION 'SYNTHETIC_COMMIT_FAILURE'; END $$;
                    CREATE CONSTRAINT TRIGGER reject_deferred_plan AFTER INSERT ON test_executions
                    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION reject_deferred_plan()
                    """);
            assertThatThrownBy(() -> repository.prepare(next)).isInstanceOf(ScanOperationException.class);
            assertThat(database.jdbc().queryForObject("SELECT count(*) FROM test_executions WHERE scan_id=?", Long.class, next)).isZero();
            assertThat(server.received).isEmpty();
        }
    }

    private UUID target(ScanTestDatabase database, ConfigurableApplicationContext context, String url, String auth) {
        UUID project = context.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("security fixture", null)).id();
        byte[] material = auth.equals("NONE") ? null : context.getBean(CredentialEncryptionPort.class).encrypt("fixture-secret").bytes();
        database.jdbc().update("""
                INSERT INTO target_configurations(id, project_id, url, request_template, response_path, headers,
                    auth_type, auth_header_name, credential_ciphertext, timeout_ms, created_at, updated_at)
                VALUES (?, ?, ?, '{"m":"{{PROMPT}}","known":"fixture-secret"}', '$.text', '{}',
                    ?, ?, ?, 1000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), project, url, auth, auth.equals("API_KEY_HEADER") ? "X-API-Key" : null, material);
        return project;
    }

    private UUID run(ConfigurableApplicationContext context, UUID project) {
        UUID scan = context.getBean(ScanLifecyclePort.class).create(project).id();
        context.getBean(ScanWorkerEntry.class).execute(scan);
        return scan;
    }

    private void assertNoEvaluation(ScanTestDatabase database) {
        assertThat(database.jdbc().queryForObject("SELECT count(*) FROM findings", Long.class)).isZero();
        assertThat(database.jdbc().queryForObject("SELECT count(*) FROM test_executions WHERE evaluator_result IS NOT NULL OR confidence IS NOT NULL", Long.class)).isZero();
        assertThat(database.jdbc().queryForObject("SELECT count(*) FROM scans WHERE global_score IS NOT NULL", Long.class)).isZero();
    }
}
