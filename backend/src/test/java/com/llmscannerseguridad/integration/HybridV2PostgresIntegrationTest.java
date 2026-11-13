package com.llmscannerseguridad.integration;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import com.llmscannerseguridad.adapters.in.rest.target.TargetRequestMapper;
import com.llmscannerseguridad.adapters.out.evaluation.EvaluationJson;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.application.execution.TestExecutionRepositoryPort;
import com.llmscannerseguridad.application.project.*;
import com.llmscannerseguridad.application.reporting.GetSecurityReportUseCase;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.application.scoring.GetScanScoreUseCase;
import com.llmscannerseguridad.application.target.*;
import com.llmscannerseguridad.config.ScanWorkerEntry;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "SCAN_TEST_DB_URL", matches = "jdbc:postgresql://localhost:5432/llm_scanner_seguridad_test")
@Timeout(120)
class HybridV2PostgresIntegrationTest {
    private static final String CANARY = "CANARIO_SINTETICO_7D4E9C2B0A61";
    @TempDir Path directory;

    @Test void sameResponseWithAndWithoutBoundCanaryClosesBothAdapterPipelines() throws Exception {
        var requests = new AtomicInteger();
        var calls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                requests.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                Object response = exchange.getRequestURI().getPath().equals("/chat")
                        ? Map.of("choices", List.of(Map.of("message", Map.of("content", CANARY), "finish_reason", "stop")))
                        : Map.of("text", CANARY);
                byte[] bytes = EvaluationJson.write(response).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        try (var db = new ScanTestDatabase()) {
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            Path file = directory.resolve("v2.json");
            var app = db.openSecurity(Map.of("TARGET_CREDENTIAL_ENCRYPTION_KEY", OpenAiCompatiblePostgresIntegrationTest.KEY,
                    "TARGET_LOCAL_DEMO_ENABLED", true, "TARGET_LOCAL_DEMO_ORIGINS", origin,
                    "EVALUATION_SCENARIO_FILE", file.toString(), "logging.level.root", "ERROR"), ctx ->
                    ctx.registerBean("forbiddenExternalEvaluator", EvaluatorPort.class, () -> (input, budget) -> {
                        calls.incrementAndGet();
                        throw new AssertionError("La fixture no admite llamadas al evaluator.");
                    }, bean -> bean.setPrimary(true)));
            for (boolean chat : List.of(false, true)) {
                UUID project = app.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("Fixture v2 " + chat, null)).id();
                String targetBody = chat ? OpenAiCompatiblePostgresIntegrationTest.CHAT.replace("https://chat.example.invalid/completions", origin + "/chat")
                        : OpenAiCompatiblePostgresIntegrationTest.REST.replace("https://rest.example.invalid/chat", origin + "/rest");
                app.getBean(ConfigureTargetUseCase.class).execute(project, new TargetRequestMapper().command(targetBody));
                var target = app.getBean(TargetConfigurationRepositoryPort.class).findByProjectId(project).orElseThrow();
                for (boolean withContext : List.of(false, true)) {
                    var entries = new ArrayList<Map<String, Object>>();
                    entries.add(entry("LLM01-INSTRUCTION-001", Map.of("restriction", "No ejecutar una acción sintética reservada")));
                    if (withContext) entries.add(entry("LLM02-CANARY-001", Map.of("reservedValue", CANARY)));
                    Files.writeString(file, EvaluationJson.write(Map.of("schemaVersion", 2, "scenarios", List.of(Map.of(
                            "scenarioId", "fixture-v2", "revision", "1", "projectId", project, "targetConfigurationId", target.id(),
                            "targetUpdatedAt", target.updatedAt().toString(), "entries", entries)))));
                    var scan = app.getBean(ScanLifecyclePort.class).create(project);
                    app.getBean(ScanWorkerEntry.class).execute(scan.id());
                    assertThat(app.getBean(GetScanUseCase.class).execute(project, scan.id()).status()).isEqualTo(ScanStatus.COMPLETED);
                    var executions = app.getBean(TestExecutionRepositoryPort.class).findByScanId(scan.id());
                    assertThat(executions).hasSize(4).allSatisfy(e -> {
                        assertThat(e.responseText()).isEqualTo(CANARY);
                        assertThat(e.responseRedacted()).isFalse();
                    });
                    var repo = app.getBean(EvaluationRepositoryPort.class);
                    var rows = repo.listEvaluations(scan.id()).items();
                    assertThat(rows).hasSize(4).allSatisfy(e -> assertThat(e.status()).isEqualTo(EvaluationStatus.COMPLETED));
                    var llm02 = rows.stream().filter(e -> e.category().name().equals("LLM02")).findFirst().orElseThrow();
                    assertThat(llm02.finalVerdict()).isEqualTo(withContext ? Verdict.VULNERABLE : Verdict.INCONCLUSIVE);
                    assertThat(llm02.decisionCode()).isEqualTo(withContext ? "HYB-02" : "HYB-01");
                    assertThat(llm02.outcomeReason()).isEqualTo(withContext ? null : OutcomeReason.CONTEXT_UNAVAILABLE);
                    var stored = repo.findByExecution(llm02.testExecutionId()).orElseThrow();
                    assertThat(stored.configuration().policyVersion()).isEqualTo("hybrid-v2");
                    assertThat(stored.evidence().projection()).isNull();
                    assertThat(stored.llm().status()).isEqualTo(LlmStatus.SKIPPED);
                    assertThat(stored.llm().attemptCount()).isZero();
                    assertThat(stored.llm().reservedCostUsd()).isNull();
                    assertThat(stored.llm().errorReason()).isNull();
                    assertThat(stored.decision().degraded()).isTrue();
                    assertThat(repo.listFindings(scan.id())).hasSize(withContext ? 1 : 0);
                    if (withContext) assertThat(stored.deterministicSignals()).anySatisfy(s -> assertThat(s.fullMatch()).isNotNull());
                    assertThat(app.getBean(GetScanScoreUseCase.class).execute(project, scan.id()).score().globalScore())
                            .satisfies(score -> { if (withContext) assertThat(score).isPositive(); else assertThat(score).isZero(); });
                    var report = app.getBean(GetSecurityReportUseCase.class).execute(project, scan.id()).report();
                    assertThat(report.reportVersion()).isEqualTo("report-v1");
                    assertThat(report.evaluations()).allSatisfy(e -> assertThat(e.versions().policyVersion()).isEqualTo("hybrid-v2"));
                }
            }
            assertThat(requests).hasValue(16);
            assertThat(calls).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    private static Map<String, Object> entry(String code, Map<String, Object> context) {
        return Map.of("securityTestCode", code, "securityTestRevision", 1, "context", context);
    }
}
