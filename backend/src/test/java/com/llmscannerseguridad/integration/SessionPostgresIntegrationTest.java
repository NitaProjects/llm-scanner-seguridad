package com.llmscannerseguridad.integration;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import com.llmscannerseguridad.adapters.in.rest.target.TargetRequestMapper;
import com.llmscannerseguridad.adapters.out.evaluation.EvaluationJson;
import com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort;
import com.llmscannerseguridad.application.execution.TestExecutionRepositoryPort;
import com.llmscannerseguridad.application.project.CreateProjectCommand;
import com.llmscannerseguridad.application.project.CreateProjectUseCase;
import com.llmscannerseguridad.application.reporting.GetSecurityReportUseCase;
import com.llmscannerseguridad.application.scan.GetScanUseCase;
import com.llmscannerseguridad.application.scan.ScanLifecyclePort;
import com.llmscannerseguridad.application.scoring.GetScanScoreUseCase;
import com.llmscannerseguridad.application.target.ConfigureTargetUseCase;
import com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort;
import com.llmscannerseguridad.config.ScanWorkerEntry;
import com.llmscannerseguridad.domain.evaluation.EvaluationStatus;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "SCAN_TEST_DB_URL", matches = "jdbc:postgresql://localhost:5432/llm_scanner_seguridad_test")
@Timeout(60)
class SessionPostgresIntegrationTest {
    @TempDir Path directory;

    @Test void fourIndependentSessionsPersistOnlyFinalRedactedResultsAndCloseScan() throws Exception {
        var bootstraps = new AtomicInteger();
        var posts = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                boolean bootstrap = exchange.getRequestURI().getPath().equals("/bootstrap");
                String response;
                if (bootstrap) {
                    int number = bootstraps.incrementAndGet();
                    exchange.getResponseHeaders().set("Set-Cookie", "fixture=SYNTHETIC_COOKIE_" + number + "; Path=/; HttpOnly");
                    response = EvaluationJson.write(Map.of("chatId", "chat-" + number, "session", Map.of("id", "session-" + number)));
                } else {
                    int number = posts.incrementAndGet();
                    if (!exchange.getRequestMethod().equals("POST") || !exchange.getRequestURI().getPath().equals("/chats/chat-" + number)
                            || !request.contains("\"session\":\"session-" + number + "\"") || exchange.getRequestHeaders().containsKey("Cookie")) {
                        exchange.sendResponseHeaders(400, -1);
                        return;
                    }
                    response = EvaluationJson.write(Map.of("text", "SYNTHETIC_COOKIE_" + number));
                }
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(bootstrap ? 200 : 201, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        try (var db = new ScanTestDatabase()) {
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            Path file = directory.resolve("sessions.json");
            var app = db.openSecurity(Map.of("TARGET_LOCAL_DEMO_ENABLED", true, "TARGET_LOCAL_DEMO_ORIGINS", origin,
                    "TARGET_SESSION_BINDINGS_FILE", file.toString(), "logging.level.root", "ERROR"));
            var project = app.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("Session fixture", null)).id();
            String request = EvaluationJson.write(Map.of("url", origin + "/answer", "requestTemplate",
                    "{\"message\":\"{{PROMPT}}\",\"session\":\"{{sessionId}}\"}", "responsePath", "$.text", "authType", "NONE"));
            app.getBean(ConfigureTargetUseCase.class).execute(project, new TargetRequestMapper().command(request));
            var target = app.getBean(TargetConfigurationRepositoryPort.class).findByProjectId(project).orElseThrow();
            Files.writeString(file, EvaluationJson.write(Map.of("schemaVersion", 1, "bindings", List.of(Map.of(
                    "projectId", project, "targetConfigurationId", target.id(), "targetUpdatedAt", target.updatedAt().toString(),
                    "bootstrap", Map.of("method", "GET", "path", "/bootstrap"), "promptPath", "/chats/{{chatId}}",
                    "captures", List.of(capture("chatId", "JSON", "$.chatId", false),
                            capture("sessionId", "JSON", "$.session.id", false), capture("cookieHandle", "COOKIE", "fixture", true)))))));
            var scan = app.getBean(ScanLifecyclePort.class).create(project);
            app.getBean(ScanWorkerEntry.class).execute(scan.id());
            assertThat(bootstraps).hasValue(4);
            assertThat(posts).hasValue(4);
            assertThat(app.getBean(GetScanUseCase.class).execute(project, scan.id()).status()).isEqualTo(ScanStatus.COMPLETED);
            assertThat(app.getBean(TestExecutionRepositoryPort.class).findByScanId(scan.id())).hasSize(4).allSatisfy(e -> {
                assertThat(e.httpStatus()).isEqualTo(201);
                assertThat(e.responseText()).isEqualTo("[REDACTED_CREDENTIAL]");
                assertThat(e.responseRedacted()).isTrue();
                assertThat(e.requestBody()).contains("session-").doesNotContain("SYNTHETIC_COOKIE", "chatId");
            });
            var evaluations = app.getBean(EvaluationRepositoryPort.class);
            assertThat(evaluations.listEvaluations(scan.id()).items()).hasSize(4)
                    .allSatisfy(e -> assertThat(e.status()).isEqualTo(EvaluationStatus.COMPLETED));
            assertThat(evaluations.listFindings(scan.id())).isEmpty();
            assertThat(app.getBean(GetScanScoreUseCase.class).execute(project, scan.id()).score().scoringVersion()).isEqualTo("scoring-v1");
            assertThat(app.getBean(GetSecurityReportUseCase.class).execute(project, scan.id()).report().reportVersion()).isEqualTo("report-v1");
        } finally {
            server.stop(0);
        }
    }

    private static Map<String, Object> capture(String name, String source, String selector, boolean sensitive) {
        return Map.of("name", name, "source", source, "selector", selector, "type", "STRING", "required", true,
                "sensitive", sensitive, "maxBytes", 256);
    }
}
