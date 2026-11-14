package com.llmscannerseguridad.integration;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfEnvironmentVariable(named = "SCAN_TEST_DB_URL", matches = "jdbc:postgresql://localhost:5432/llm_scanner_seguridad_test")
@Timeout(90)
class WorkflowPostgresIntegrationTest {
    private static final String COOKIE = "fixture-session-cookie";
    private static final String CSRF = "fixture-csrf-handle";
    @TempDir Path directory;

    @Test void workflowClosesScanAndLegacyHistoryRemainsReadableWithoutBinding() throws Exception {
        var chats = new AtomicInteger();
        var sessions = new AtomicInteger();
        var answers = new AtomicInteger();
        var legacy = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                String path = exchange.getRequestURI().getPath();
                String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String media = "application/json";
                String response;
                int status = 200;
                if (path.equals("/legacy")) {
                    legacy.incrementAndGet();
                    response = "{\"text\":\"respuesta histórica\"}";
                } else if (path.equals("/api/chats")) {
                    exchange.getResponseHeaders().add("Set-Cookie", "fixture=" + COOKIE + "; Path=/api; HttpOnly");
                    exchange.getResponseHeaders().add("X-CSRF-Token", CSRF);
                    response = "{\"chatId\":\"chat-" + chats.incrementAndGet() + "\",\"privateValue\":\"fixture-private-value\"}";
                } else if (path.equals("/api/chats/chat-" + chats.get() + "/sessions") && credentialsPresent(exchange)) {
                    response = "{\"sessionId\":\"session-" + sessions.incrementAndGet() + "\"}";
                } else if (path.equals("/api/chats/chat-" + chats.get() + "/sessions/session-" + sessions.get() + "/answer")
                        && "text/event-stream".equals(exchange.getRequestHeaders().getFirst("Accept")) && request.contains("\"prompt\":")
                        && credentialsPresent(exchange)) {
                    answers.incrementAndGet();
                    media = "text/event-stream";
                    response = "data: {\"delta\":\"respuesta terminal fixture-private-value " + COOKIE + " " + CSRF + "\"}\n\ndata: [DONE]\n\n";
                    status = 201;
                } else {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", media);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        try (var db = new ScanTestDatabase()) {
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            Path file = directory.resolve("workflows.json");
            Files.writeString(file, "{\"schemaVersion\":1,\"bindings\":[]}");
            var app = db.openSecurity(Map.of("TARGET_LOCAL_DEMO_ENABLED", true, "TARGET_LOCAL_DEMO_ORIGINS", origin,
                    "TARGET_WORKFLOW_BINDINGS_FILE", file.toString(), "logging.level.root", "ERROR"));
            var project = app.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("Workflow fixture", null)).id();
            String request = EvaluationJson.write(Map.of("url", origin + "/legacy", "requestTemplate",
                    "{\"message\":\"{{PROMPT}}\"}", "responsePath", "$.text", "authType", "NONE"));
            app.getBean(ConfigureTargetUseCase.class).execute(project, new TargetRequestMapper().command(request));
            var target = app.getBean(TargetConfigurationRepositoryPort.class).findByProjectId(project).orElseThrow();
            var previous = app.getBean(ScanLifecyclePort.class).create(project);
            app.getBean(ScanWorkerEntry.class).execute(previous.id());
            assertThat(legacy).hasValue(4);
            var previousReport = app.getBean(GetSecurityReportUseCase.class).execute(project, previous.id()).report();
            var previousScore = app.getBean(GetScanScoreUseCase.class).execute(project, previous.id()).score();
            Files.writeString(file, EvaluationJson.write(Map.of("schemaVersion", 1, "bindings", List.of(Map.of(
                    "projectId", project, "targetConfigurationId", target.id(), "targetUpdatedAt", target.updatedAt().toString(),
                    "workflowId", "fixture", "revision", 1, "allowedDynamicHeaders", List.of("X-CSRF-Token"), "steps", steps())))));
            var scan = app.getBean(ScanLifecyclePort.class).create(project);
            app.getBean(ScanWorkerEntry.class).execute(scan.id());
            assertThat(chats).hasValue(4);
            assertThat(sessions).hasValue(4);
            assertThat(answers).hasValue(4);
            db.closeApplication(app);
            var reader = db.openSecurity(Map.of("TARGET_WORKFLOW_BINDINGS_FILE", "", "logging.level.root", "ERROR"));
            assertThat(reader.getBean(GetScanUseCase.class).execute(project, scan.id()).status()).isEqualTo(ScanStatus.COMPLETED);
            var executions = reader.getBean(TestExecutionRepositoryPort.class);
            assertThat(executions.findByScanId(previous.id())).hasSize(4).allSatisfy(e -> assertThat(e.responseText()).isEqualTo("respuesta histórica"));
            assertThat(executions.findByScanId(scan.id())).hasSize(4).allSatisfy(e -> {
                assertThat(e.httpStatus()).isEqualTo(201);
                assertThat(e.responseText()).isEqualTo("respuesta terminal [REDACTED_CREDENTIAL] [REDACTED_CREDENTIAL] [REDACTED_CREDENTIAL]");
                assertThat(e.responseRedacted()).isTrue();
                assertThat(e.requestBody()).contains("\"prompt\"").doesNotContain("fixture-private-value", "chat-", "session-", "chatId", "sessionId", COOKIE, CSRF);
            });
            assertThat(reader.getBean(EvaluationRepositoryPort.class).listEvaluations(scan.id()).items()).hasSize(4)
                    .allSatisfy(e -> assertThat(e.status()).isEqualTo(EvaluationStatus.COMPLETED));
            var scores = reader.getBean(GetScanScoreUseCase.class);
            var reports = reader.getBean(GetSecurityReportUseCase.class);
            assertThat(scores.execute(project, scan.id()).score().scoringVersion()).isEqualTo("scoring-v1");
            assertThat(reports.execute(project, scan.id()).report().reportVersion()).isEqualTo("report-v1");
            assertThat(scores.execute(project, previous.id()).score()).usingRecursiveComparison().isEqualTo(previousScore);
            assertThat(reports.execute(project, previous.id()).report()).usingRecursiveComparison().isEqualTo(previousReport);
            for (String table : List.of("test_executions", "evaluation_results", "findings", "scan_scores", "security_reports")) {
                Integer exposed = db.jdbc().queryForObject("select count(*) from " + table
                        + " as row_data where strpos(to_jsonb(row_data)::text, ?) > 0 or strpos(to_jsonb(row_data)::text, ?) > 0",
                        Integer.class, COOKIE, CSRF);
                assertThat(exposed).as(table).isZero();
            }
        } finally {
            server.stop(0);
        }
    }

    private static List<Map<String, Object>> steps() {
        var credentials = List.of(Map.of("capture", "sessionHandle", "destination", "COOKIE", "name", "fixture"),
                Map.of("capture", "csrfHandle", "destination", "HEADER", "name", "X-CSRF-Token"));
        return List.of(Map.of("id", "chat", "method", "POST", "path", "/api/chats", "body", Map.of(),
                        "dependsOn", List.of(), "response", Map.of("type", "JSON"),
                        "captures", List.of(capture("chatId", "$.chatId", false), capture("privateHandle", "$.privateValue", true),
                                transportCapture("sessionHandle", "COOKIE", "fixture"), transportCapture("csrfHandle", "HEADER", "X-CSRF-Token"))),
                Map.of("id", "session", "method", "POST", "path", "/api/chats/{{chatId}}/sessions", "body", Map.of(),
                        "dependsOn", List.of("chat"), "response", Map.of("type", "JSON"),
                        "credentialBindings", credentials,
                        "captures", List.of(capture("sessionId", "$.sessionId", false))),
                Map.of("id", "answer", "method", "POST", "path", "/api/chats/{{chatId}}/sessions/{{sessionId}}/answer",
                        "body", Map.of("prompt", "{{PROMPT}}"), "dependsOn", List.of("chat", "session"),
                        "credentialBindings", credentials,
                        "response", Map.of("type", "SSE", "stream", Map.of("mediaType", "text/event-stream",
                                "textPath", "$.delta", "mode", "DELTA", "terminalLiteral", "[DONE]"))));
    }

    private static boolean credentialsPresent(com.sun.net.httpserver.HttpExchange exchange) {
        return ("fixture=" + COOKIE).equals(exchange.getRequestHeaders().getFirst("Cookie"))
                && CSRF.equals(exchange.getRequestHeaders().getFirst("X-CSRF-Token"));
    }

    private static Map<String, Object> transportCapture(String name, String source, String selector) {
        return Map.of("name", name, "source", source, "selector", selector, "type", "STRING",
                "required", true, "sensitive", true, "maxBytes", 128);
    }

    private static Map<String, Object> capture(String name, String path, boolean sensitive) {
        return Map.of("name", name, "source", "JSON", "selector", path, "type", "STRING",
                "required", true, "sensitive", sensitive, "maxBytes", 128);
    }
}
