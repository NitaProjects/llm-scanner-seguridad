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
class StreamingPostgresIntegrationTest {
    @TempDir Path directory;

    @Test void workerFixesStreamingBindingAndPersistsOnlyFinalTextBeforeEvaluationAndReport() throws Exception {
        var requests = new AtomicInteger();
        Path file = directory.resolve("stream.json");
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                if (!List.of("text/event-stream").equals(exchange.getRequestHeaders().get("Accept"))) {
                    exchange.sendResponseHeaders(406, -1);
                    return;
                }
                if (requests.incrementAndGet() == 1) Files.writeString(file, "{}");
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write(("event: content\ndata: {\"text\":\"SA\"}\n\n"
                        + "event: content\ndata: {\"text\":\"FE\"}\n\nevent: end\n\n").getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();
        try (var db = new ScanTestDatabase()) {
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            var app = db.openSecurity(Map.of("TARGET_LOCAL_DEMO_ENABLED", true, "TARGET_LOCAL_DEMO_ORIGINS", origin,
                    "TARGET_STREAM_BINDINGS_FILE", file.toString(), "logging.level.root", "ERROR"));
            var project = app.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("Streaming fixture", null)).id();
            app.getBean(ConfigureTargetUseCase.class).execute(project, new TargetRequestMapper().command(
                    OpenAiCompatiblePostgresIntegrationTest.REST.replace("https://rest.example.invalid/chat", origin)));
            var target = app.getBean(TargetConfigurationRepositoryPort.class).findByProjectId(project).orElseThrow();
            Files.writeString(file, EvaluationJson.write(Map.of("schemaVersion", 1, "bindings", List.of(Map.of(
                    "projectId", project, "targetConfigurationId", target.id(), "targetUpdatedAt", target.updatedAt().toString(),
                    "mediaType", "text/event-stream", "textPath", "$.text", "contentEvent", "content", "terminalEvent", "end", "mode", "DELTA")))));
            var scan = app.getBean(ScanLifecyclePort.class).create(project);
            app.getBean(ScanWorkerEntry.class).execute(scan.id());
            assertThat(requests).hasValue(4);
            assertThat(app.getBean(GetScanUseCase.class).execute(project, scan.id()).status()).isEqualTo(ScanStatus.COMPLETED);
            assertThat(app.getBean(TestExecutionRepositoryPort.class).findByScanId(scan.id())).hasSize(4).allSatisfy(e -> {
                assertThat(e.responseText()).isEqualTo("SAFE");
                assertThat(e.httpStatus()).isEqualTo(200);
                assertThat(e.errorReason()).isNull();
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
}
