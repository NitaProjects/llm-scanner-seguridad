package com.llmscannerseguridad.integration;

import static org.assertj.core.api.Assertions.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import com.llmscannerseguridad.adapters.in.rest.target.TargetRequestMapper;
import com.llmscannerseguridad.adapters.out.evaluation.EvaluationJson;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.application.project.*;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.application.scoring.GetScanScoreUseCase;
import com.llmscannerseguridad.application.reporting.GetSecurityReportUseCase;
import com.llmscannerseguridad.application.target.*;
import com.llmscannerseguridad.config.ScanWorkerEntry;
import com.llmscannerseguridad.domain.scan.*;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.privacy.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfEnvironmentVariable(named="SCAN_TEST_DB_URL", matches="jdbc:postgresql://localhost:5432/llm_scanner_seguridad_test")
@Timeout(180)
class DurableSecretPostgresIntegrationTest {
    private static final String SECRET = "fixture-durable-private-91";
    @TempDir Path directory;

    @Test void realRestChatSseJsonlCommitsAreAuditedAfterReopenWithoutScopes() throws Exception {
        var requests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                if (!("Bearer " + SECRET).equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                    exchange.sendResponseHeaders(401, -1); return;
                }
                int number = requests.getAndIncrement() % 4;
                String text = number == 0 ? Character.toString(92) + "u0066" + SECRET.substring(1) : encode(SECRET, number == 3 ? 4 : number);
                String path = exchange.getRequestURI().getPath(), media = "application/json", response;
                if (path.equals("/chat")) response = EvaluationJson.write(Map.of("choices", List.of(Map.of(
                        "message", Map.of("content", text), "finish_reason", "stop"))));
                else if (path.equals("/sse")) {
                    media = "text/event-stream";
                    response = "event: content\ndata: " + EvaluationJson.write(Map.of("text", text)) + "\n\nevent: end\n\n";
                } else if (path.equals("/jsonl")) {
                    media = "application/x-ndjson";
                    response = EvaluationJson.write(Map.of("text", text)) + "\n{\"done\":true}\n";
                } else response = EvaluationJson.write(Map.of("text", text));
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", media);
                exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        try (var db = new ScanTestDatabase()) {
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            Path bindings = directory.resolve("streams.json");
            Files.writeString(bindings, "{\"schemaVersion\":1,\"bindings\":[]}");
            var key = new byte[32]; new java.security.SecureRandom().nextBytes(key);
            var app = db.openSecurity(Map.of("TARGET_LOCAL_DEMO_ENABLED", true, "TARGET_LOCAL_DEMO_ORIGINS", origin,
                    "TARGET_STREAM_BINDINGS_FILE", bindings.toString(), "TARGET_CREDENTIAL_ENCRYPTION_KEY", Base64.getEncoder().encodeToString(key),
                    "logging.level.root", "ERROR"));
            var completed = new LinkedHashMap<UUID, UUID>();
            for (String mode : List.of("json", "chat", "sse", "jsonl")) {
                UUID project = app.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("Durable fixture " + mode, null)).id();
                var targetInput = new LinkedHashMap<String, Object>();
                targetInput.put("adapterType", mode.equals("chat") ? "OPENAI_COMPATIBLE" : "REST_LEGACY");
                targetInput.put(mode.equals("chat") ? "endpoint" : "url", origin + "/" + mode);
                targetInput.put("authType", "BEARER_TOKEN"); targetInput.put("authConfig", Map.of("secretValue", SECRET)); targetInput.put("timeoutMs", 20000);
                if (mode.equals("chat")) targetInput.put("model", "fixture-model");
                else { targetInput.put("requestTemplate", "{\"prompt\":\"{{PROMPT}}\"}"); targetInput.put("responsePath", "$.text"); }
                app.getBean(ConfigureTargetUseCase.class).execute(project, new TargetRequestMapper().command(EvaluationJson.write(targetInput)));
                var target = app.getBean(TargetConfigurationRepositoryPort.class).findByProjectId(project).orElseThrow();
                if (mode.equals("sse") || mode.equals("jsonl")) {
                    var binding = new LinkedHashMap<String, Object>();
                    binding.put("projectId", project); binding.put("targetConfigurationId", target.id()); binding.put("targetUpdatedAt", target.updatedAt().toString());
                    binding.put("mediaType", mode.equals("sse") ? "text/event-stream" : "application/x-ndjson");
                    binding.put("textPath", "$.text"); binding.put("mode", "DELTA");
                    if (mode.equals("sse")) { binding.put("contentEvent", "content"); binding.put("terminalEvent", "end"); }
                    else binding.put("terminalLiteral", "{\"done\":true}");
                    Files.writeString(bindings, EvaluationJson.write(Map.of("schemaVersion", 1, "bindings", List.of(binding))));
                } else Files.writeString(bindings, "{\"schemaVersion\":1,\"bindings\":[]}");
                var scan = app.getBean(ScanLifecyclePort.class).create(project);
                app.getBean(ScanWorkerEntry.class).execute(scan.id()); completed.put(project, scan.id());
                var finished = app.getBean(GetScanUseCase.class).execute(project, scan.id());
                var executions = app.getBean(TestExecutionRepositoryPort.class).findByScanId(scan.id());
                // Solo metadata tipada: nunca bodies, credenciales ni el toString de la ejecución.
                assertThat(finished.status()).as("%s scanError=%s executions=%s", mode, finished.errorReason(),
                        executions.stream().map(row -> row.securityTestId() + ":" + row.status() + "/" + row.errorReason()
                                + "/http=" + row.httpStatus() + "/requestRedacted=" + row.requestRedacted()
                                + "/responseRedacted=" + row.responseRedacted()).toList()).isEqualTo(ScanStatus.COMPLETED);
                assertThat(executions).hasSize(4).allSatisfy(row -> {
                    assertThat(row.responseRedacted()).isTrue(); assertThat(row.errorReason()).isNull();
                    assertThat(IndependentPrivacyOracle.absent(row.responseText(), List.of(SECRET))).isTrue();
                });
                assertThat(app.getBean(EvaluationRepositoryPort.class).listEvaluations(scan.id()).items()).hasSize(4)
                        .allSatisfy(row -> assertThat(row.status()).isEqualTo(EvaluationStatus.COMPLETED));
                try (var connection = db.connection()) { IndependentPrivacyOracle.audit(connection, List.of(SECRET)); }
            }
            assertThat(requests).hasValue(16); assertThat(ProtectionScope.active()).isFalse();
            assertThat(db.jdbc().queryForObject("select count(*) from target_configurations where credential_ciphertext is not null", Integer.class)).isEqualTo(4);
            db.closeApplication(app);
            var reopened = db.openSecurity(Map.of("logging.level.root", "ERROR"));
            for (var entry : completed.entrySet()) {
                assertThat(reopened.getBean(GetScanScoreUseCase.class).execute(entry.getKey(), entry.getValue()).score().scoringVersion()).isEqualTo("scoring-v1");
                assertThat(reopened.getBean(GetSecurityReportUseCase.class).execute(entry.getKey(), entry.getValue()).report().reportVersion()).isEqualTo("report-v1");
            }
            try (var connection = db.connection()) { IndependentPrivacyOracle.audit(connection, List.of(SECRET)); }
            assertThat(requests).hasValue(16); assertThat(ProtectionScope.active()).isFalse();
        } finally { server.stop(0); }
    }

    static String encode(String value, int layers) {
        for (int i = 0; i < layers; i++) value = EvaluationJson.write(value);
        return value;
    }

    @Test void workflowCookiesCsrfRetrievalToolsAndDiscardedIdsNeverReachDurableCopies() throws Exception {
        String cookie = "fixture-cookie-private-72", csrf = "fixture-csrf-private-72", session = "fixture-session-private-72";
        String retrieval = "fixture-retrieval-private-72", argument = "fixture-argument-private-72", result = "fixture-result-private-72";
        String call = "fixture-correlation-private-72", oversized = "z".repeat(129);
        var sentinels = List.of(cookie, csrf, session, retrieval, argument, result, call, oversized);
        var requests = new AtomicInteger(); var bad = new java.util.concurrent.atomic.AtomicBoolean();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes(); requests.incrementAndGet();
                String path = exchange.getRequestURI().getPath(), response, media = "application/json";
                if (path.equals("/api/chats")) {
                    exchange.getResponseHeaders().add("Set-Cookie", "fixture=" + cookie + "; Path=/api; HttpOnly");
                    exchange.getResponseHeaders().add("X-CSRF-Token", csrf);
                    response = EvaluationJson.write(Map.of("chatId", "chat-1", "privateValue", session));
                } else if (!("fixture=" + cookie).equals(exchange.getRequestHeaders().getFirst("Cookie"))
                        || !csrf.equals(exchange.getRequestHeaders().getFirst("X-CSRF-Token"))) {
                    exchange.sendResponseHeaders(400, -1); return;
                } else if (path.endsWith("/sessions")) response = EvaluationJson.write(Map.of("sessionId", "session-1", "retrieval", retrieval));
                else {
                    String id = bad.get() ? oversized : call;
                    String output = "Nota: " + encode(String.join(" ", cookie, csrf, session, retrieval, argument, result, id), 3);
                    response = "event: request\ndata: " + EvaluationJson.write(Map.of("id", id, "tool", "fixture_tool", "argument", argument))
                            + "\n\nevent: result\ndata: " + EvaluationJson.write(Map.of("id", id, "result", result))
                            + "\n\nevent: answer\ndata: " + EvaluationJson.write(Map.of("delta", output)) + "\n\nevent: end\n\n";
                    media = "text/event-stream";
                }
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", media); exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        }); server.start();
        try (var db = new ScanTestDatabase()) {
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            Path file = directory.resolve("workflow.json"); Files.writeString(file, "{\"schemaVersion\":1,\"bindings\":[]}");
            var app = db.openSecurity(Map.of("TARGET_LOCAL_DEMO_ENABLED", true, "TARGET_LOCAL_DEMO_ORIGINS", origin,
                    "TARGET_WORKFLOW_BINDINGS_FILE", file.toString(), "logging.level.root", "ERROR"));
            UUID project = app.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("Durable workflow", null)).id();
            app.getBean(ConfigureTargetUseCase.class).execute(project, new TargetRequestMapper().command(EvaluationJson.write(Map.of(
                    "url", origin + "/legacy", "requestTemplate", "{\"prompt\":\"{{PROMPT}}\"}", "responsePath", "$.text", "authType", "NONE"))));
            var target = app.getBean(TargetConfigurationRepositoryPort.class).findByProjectId(project).orElseThrow();
            Files.writeString(file, EvaluationJson.write(Map.of("schemaVersion", 1, "bindings", List.of(Map.of(
                    "projectId", project, "targetConfigurationId", target.id(), "targetUpdatedAt", target.updatedAt().toString(),
                    "workflowId", "durable-fixture", "revision", 1, "allowedDynamicHeaders", List.of("X-CSRF-Token"),
                    "steps", WorkflowPostgresIntegrationTest.steps())))));
            var lifecycle = app.getBean(ScanLifecyclePort.class); UUID completed = lifecycle.create(project).id();
            app.getBean(ScanWorkerEntry.class).execute(completed);
            assertThat(app.getBean(GetScanUseCase.class).execute(project, completed).status()).isEqualTo(ScanStatus.COMPLETED);
            assertThat(requests).hasValue(12);
            try (var connection = db.connection()) { IndependentPrivacyOracle.audit(connection, sentinels); }
            bad.set(true); UUID failed = lifecycle.create(project).id(); app.getBean(ScanWorkerEntry.class).execute(failed);
            assertThat(app.getBean(GetScanUseCase.class).execute(project, failed).status()).isEqualTo(ScanStatus.FAILED);
            assertThat(requests).hasValue(15);
            assertThat(app.getBean(TestExecutionRepositoryPort.class).findByScanId(failed)).hasSize(4).allSatisfy(row -> {
                assertThat(row.errorReason()).isEqualTo(com.llmscannerseguridad.domain.execution.ExecutionError.EVIDENCE_LIMIT_EXCEEDED);
                assertThat(row.requestBody()).isNull(); assertThat(row.responseText()).isNull();
                assertThat(row.requestRedacted()).isFalse(); assertThat(row.responseRedacted()).isFalse();
            });
            db.closeApplication(app); var reader = db.openSecurity(Map.of("logging.level.root", "ERROR"));
            assertThat(reader.getBean(GetSecurityReportUseCase.class).execute(project, completed).report().reportVersion()).isEqualTo("report-v1");
            for (var execution : reader.getBean(TestExecutionRepositoryPort.class).findByScanId(failed)) {
                var evaluation = reader.getBean(EvaluationRepositoryPort.class).findByExecution(execution.id()).orElseThrow();
                assertThat(evaluation.status()).isEqualTo(EvaluationStatus.FAILED);
                assertThat(evaluation.decision().confidence()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
            }
            assertThat(reader.getBean(EvaluationRepositoryPort.class).listEvaluations(completed).items()).hasSize(4);
            try (var connection = db.connection()) { IndependentPrivacyOracle.audit(connection, sentinels); }
            assertThat(requests).hasValue(15); assertThat(ProtectionScope.active()).isFalse();
        } finally { server.stop(0); }
    }

    @Test void secondaryCopiesAndReportAreAuditedAndReportFailureRollsBackJointClosure() throws Exception {
        try (var db = new ScanTestDatabase()) {
            var app = db.openSecurity(Map.of("logging.level.root", "ERROR"));
            UUID project = project(app);
            var lifecycle = app.getBean(ScanLifecyclePort.class);
            UUID completed = lifecycle.create(project).id();
            try (var scope = ProtectionScope.open(completed, 524288)) {
                ProtectionScope.register(SECRET);
                completeRows(app, lifecycle, completed);
                assertThat(lifecycle.complete(completed)).isTrue();
            }
            assertThat(app.getBean(EvaluationRepositoryPort.class).listFindings(completed)).hasSize(4);
            var priorReport = app.getBean(GetSecurityReportUseCase.class).execute(project, completed).report();
            try (var connection = db.connection()) { IndependentPrivacyOracle.audit(connection, List.of(SECRET)); }

            UUID failing = lifecycle.create(project).id();
            try (var scope = ProtectionScope.open(failing, 524288)) {
                ProtectionScope.register(SECRET); completeRows(app, lifecycle, failing);
                db.jdbc().execute("create function fixture_report_failure() returns trigger language plpgsql as $$ begin raise exception 'fixture report rejected'; end $$");
                db.jdbc().execute("create trigger fixture_report_failure before insert on security_reports for each row execute function fixture_report_failure()");
                assertThatThrownBy(() -> lifecycle.complete(failing)).isInstanceOf(RuntimeException.class);
                assertThat(db.jdbc().queryForObject("select count(*) from scan_scores where scan_id=?", Integer.class, failing)).isZero();
                assertThat(db.jdbc().queryForObject("select count(*) from security_reports where scan_id=?", Integer.class, failing)).isZero();
                assertThat(lifecycle.fail(failing, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED)).isTrue();
            }
            db.closeApplication(app);
            var reader = db.openSecurity(Map.of("logging.level.root", "ERROR"));
            assertThat(reader.getBean(GetSecurityReportUseCase.class).execute(project, completed).report()).isEqualTo(priorReport);
            assertThat(reader.getBean(GetScanUseCase.class).execute(project, completed).status()).isEqualTo(ScanStatus.COMPLETED);
            assertThat(reader.getBean(GetScanUseCase.class).execute(project, failing).status()).isEqualTo(ScanStatus.FAILED);
            try (var connection = db.connection()) { IndependentPrivacyOracle.audit(connection, List.of(SECRET)); }
            assertThat(ProtectionScope.active()).isFalse();
        }
    }

    @Test void requiredSecondaryWriteIsRejectedBeforeBindingAndFindingFailureRollsBackEvaluation() throws Exception {
        try (var db = new ScanTestDatabase()) {
            var app = db.openSecurity(Map.of("logging.level.root", "ERROR"));
            UUID project = project(app); var lifecycle = app.getBean(ScanLifecyclePort.class);
            UUID scan = lifecycle.create(project).id();
            try (var scope = ProtectionScope.open(scan, 524288)) {
                ProtectionScope.register(SECRET); lifecycle.claim(scan).orElseThrow();
                var executions = app.getBean(TestExecutionRepositoryPort.class); var plan = executions.prepare(scan);
                var row = executions.start(plan.executions().getFirst().id());
                executions.finish(row, new InvocationResult(200, 3L, null, "{\"echo\":" + EvaluationJson.write(encode(SECRET, 2)) + "}", "SAFE", false, false));
                var repository = app.getBean(EvaluationRepositoryPort.class);
                var pending = repository.findByExecution(row.id()).orElseThrow();
                var running = start(pending, executions.findByScanId(scan).getFirst().finishedAt());
                running = repository.claim(running, false).orElseThrow();
                var c = running.configuration();
                var badConfig = new EvaluationResult.Configuration(c.criteriaId(), c.criteriaVersion(), c.scenarioId(), c.scenarioVersion(),
                        c.engineVersion(), c.policyVersion(), c.confidenceVersion(), c.severityVersion(), c.promptVersion(), c.schemaVersion(),
                        c.llmInputProfile(), EvaluationJson.write(Map.of("copy", encode(SECRET, 3))), c.configurationHash(), c.requestedProvider(), c.requestedModel(), c.samplingParameters());
                var candidate = positive(running);
                var unsafe = new EvaluationResult(candidate.id(), candidate.testExecutionId(), candidate.scanId(), candidate.securityTestId(),
                        candidate.category(), candidate.status(), candidate.createdAt(), candidate.startedAt(), candidate.finishedAt(),
                        candidate.errorReason(), candidate.decision(), candidate.deterministicSignals(), candidate.llm(), badConfig,
                        candidate.evidence(), candidate.reasoningSummary(), candidate.recommendation(), candidate.evidenceSummary());
                assertThatThrownBy(() -> repository.finish(unsafe)).isInstanceOf(IllegalStateException.class);
                assertThat(repository.findByExecution(row.id()).orElseThrow().status()).isEqualTo(EvaluationStatus.RUNNING);
                assertThat(repository.listFindings(scan)).isEmpty();
                db.jdbc().execute("create function fixture_finding_failure() returns trigger language plpgsql as $$ begin raise exception 'fixture finding rejected'; end $$");
                db.jdbc().execute("create trigger fixture_finding_failure before insert on findings for each row execute function fixture_finding_failure()");
                assertThatThrownBy(() -> repository.finish(candidate)).isInstanceOf(RuntimeException.class);
                assertThat(repository.findByExecution(row.id()).orElseThrow().status()).isEqualTo(EvaluationStatus.RUNNING);
                assertThat(repository.listFindings(scan)).isEmpty();
                try (var connection = db.connection()) { IndependentPrivacyOracle.audit(connection, List.of(SECRET)); }
                assertThat(lifecycle.fail(scan, ScanStatus.RUNNING, ScanFailureReason.SCAN_PROCESSING_FAILED)).isTrue();
            }
            db.closeApplication(app);
            var reader = db.openSecurity(Map.of("logging.level.root", "ERROR"));
            assertThat(reader.getBean(GetScanUseCase.class).execute(project, scan).status()).isEqualTo(ScanStatus.FAILED);
            try (var connection = db.connection()) { IndependentPrivacyOracle.audit(connection, List.of(SECRET)); }
        }
    }

    private static UUID project(org.springframework.context.ConfigurableApplicationContext app) {
        UUID project = app.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("Secondary fixture", null)).id();
        app.getBean(ConfigureTargetUseCase.class).execute(project, new TargetRequestMapper().command(EvaluationJson.write(Map.of(
                "url", "https://fixture.example.invalid/chat", "requestTemplate", "{\"prompt\":\"{{PROMPT}}\"}", "responsePath", "$.text", "authType", "NONE"))));
        return project;
    }
    private static void completeRows(org.springframework.context.ConfigurableApplicationContext app, ScanLifecyclePort lifecycle, UUID scan) {
        lifecycle.claim(scan).orElseThrow();
        var executions = app.getBean(TestExecutionRepositoryPort.class); var evaluations = app.getBean(EvaluationRepositoryPort.class);
        var plan = executions.prepare(scan);
        for (var entry : plan.executions()) {
            var row = executions.start(entry.id());
            executions.finish(row, new InvocationResult(200, 2L, null,
                    "{\"echo\":" + EvaluationJson.write(encode(SECRET, 2)) + "}", "SAFE", false, false));
            var pending = evaluations.findByExecution(row.id()).orElseThrow();
            var confirmed = executions.findByScanId(scan).stream().filter(e -> e.id().equals(row.id())).findFirst().orElseThrow();
            var running = evaluations.claim(start(pending, confirmed.finishedAt()), false).orElseThrow();
            evaluations.finish(positive(running));
        }
    }
    private static EvaluationResult start(EvaluationResult pending, Instant finishedAt) {
        return pending.start(Instant.now(), finishedAt, List.of(), new EvaluationResult.Evidence(
                EvaluationJson.write(Map.of("copy", encode(SECRET, 2))), true, false, 100, 100, "unused-fixture-hash"),
                new EvaluationResult.LlmComponent(LlmStatus.SKIPPED, null, 0, null, null, null, null, null, null, null, null, null, null));
    }
    private static EvaluationResult positive(EvaluationResult running) {
        var confidence = new HybridDecisionPolicy.ConfidenceComponents(new java.math.BigDecimal("0.95"), java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO, new java.math.BigDecimal("0.95"), new java.math.BigDecimal("0.95"));
        var decision = new HybridDecisionPolicy.Decision(Verdict.VULNERABLE, Verdict.VULNERABLE, "HYB-02", confidence,
                Severity.HIGH, true, false, false, null);
        String copy = Character.toString(92) + "u0066" + SECRET.substring(1);
        return running.complete(Instant.now(), decision, running.llm(), copy, copy, copy);
    }
}
