package com.llmscannerseguridad.demo;

import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.llmscannerseguridad.demo.config.CatalogLoader;
import com.llmscannerseguridad.demo.scenario.DemoProfile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class HttpContractTest {
    private static ConfigurableApplicationContext safe, vulnerable;
    private static int safePort, vulnerablePort;
    private static HttpClient client;

    @BeforeAll static void startLocalTargets() {
        safe = startTarget(DemoProfile.SAFE);
        vulnerable = startTarget(DemoProfile.VULNERABLE);
        safePort = ((WebServerApplicationContext) safe).getWebServer().getPort();
        vulnerablePort = ((WebServerApplicationContext) vulnerable).getWebServer().getPort();
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(2)).build();
    }

    private static ConfigurableApplicationContext startTarget(DemoProfile profile) {
        return new SpringApplicationBuilder(DemoApplication.class).environment(new MockEnvironment()).run(
                "--spring.config.location=classpath:/application.yml", "--DEMO_PROFILE=" + profile.name(),
                "--DEMO_BIND_ADDRESS=127.0.0.1", "--DEMO_PORT=8081", "--server.address=127.0.0.1", "--server.port=0");
    }
    @AfterAll static void stopLocalTargets() {
        if (client != null) client.close();
        if (vulnerable != null) vulnerable.close();
        if (safe != null) safe.close();
    }

    private HttpResponse<String> request(int port, String method, String path, byte[] body, String... headers) throws Exception {
        // Espaciado del cliente de test; la aplicación rechaza sin dormir.
        Thread.sleep(110);
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5)).method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        for (int i = 0; i < headers.length; i += 2) builder.header(headers[i], headers[i + 1]);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private HttpResponse<String> post(int port, byte[] body) throws Exception {
        return request(port, "POST", "/api/chat", body, "Content-Type", "application/json");
    }
    private byte[] json(String message) { return TestJson.MAPPER.writeValueAsBytes(Map.of("message", message)); }
    private void error(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(TestJson.MAPPER.readValue(response.body(), Object.class)).isEqualTo(Map.of("code", code));
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(response.headers().firstValue("Location")).isEmpty();
    }

    @Test void eightRealHttpCasesHaveExactStringExtractionAndInertHeaders() throws Exception {
        for (var profile : DemoProfile.values()) {
            int port = profile == DemoProfile.SAFE ? safePort : vulnerablePort;
            var health = request(port, "GET", "/health", new byte[0]);
            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(TestJson.MAPPER.readValue(health.body(), Object.class)).isEqualTo(
                    Map.of("status", "UP", "demo", true, "profile", profile.name(), "scenarioVersion", "1"));
            for (var scenario : CatalogLoader.load()) {
                var reply = request(port, "POST", "/api/chat", json(scenario.prompt()),
                        "Content-Type", "application/json;charset=UTF-8", "Accept-Encoding", "gzip", "DEMO_PROFILE", "OTHER");
                assertThat(reply.statusCode()).isEqualTo(200);
                assertThat(reply.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
                assertThat(reply.headers().firstValue("Content-Encoding")).isEmpty();
                assertThat(reply.headers().firstValue("Cache-Control")).contains("no-store");
                var envelope = TestJson.object(TestJson.MAPPER.readValue(reply.body(), Object.class));
                assertThat(envelope).containsOnlyKeys("response");
                assertThat(envelope.get("response")).isEqualTo(scenario.responses().get(profile)).isInstanceOf(String.class);
                if (scenario.category().equals("LLM10")) {
                    var inner = TestJson.object(TestJson.MAPPER.readValue((String) envelope.get("response"), Object.class));
                    assertThat(inner.get("text")).isEqualTo("<span data-scanner=\"demo\">SCANNER_OUTPUT_DEMO</span>");
                    if (profile == DemoProfile.SAFE) assertThat(inner).containsOnlyKeys("text");
                    else assertThat(inner).containsOnlyKeys("text", "unexpected").containsEntry("unexpected", "DEMO_OUTPUT_EXTRA_V1");
                }
            }
        }
    }

    @Test void strictRequestRejectsMalformedUnknownDuplicateAndInvalidUnicodeWithoutEcho() throws Exception {
        for (String invalid : List.of("{}", "[]", "null", "{\"message\":null}", "{\"message\":1}", "{\"message\":true}",
                "{\"message\":[]}", "{\"message\":\"\"}", "{\"message\":\" \\n\"}", "{\"message\":\"ORACLE_ECHO\",\"profile\":\"VULNERABLE\"}",
                "{\"message\":\"a\",\"message\":\"b\"}", "{\"message\":\"x\"} {}", "{\"message\":\"\\ud800\"}",
                "{\"message\":\"\\udc00\"}", "{\"message\":\"\\ud800x\"}", "{\"message\":\"x\",\"\\ud800\":1}",
                "{\"message\":{\"a\":{\"b\":{\"c\":{\"d\":0}}}}}", "not-json-ORACLE_ECHO", "")) {
            error(post(vulnerablePort, invalid.getBytes(StandardCharsets.UTF_8)), 400, "INVALID_REQUEST");
        }
        error(post(vulnerablePort, new byte[]{'{', '"', 'm', 'e', 's', 's', 'a', 'g', 'e', '"', ':', '"', (byte) 0xc3, 0x28, '"', '}'}), 400, "INVALID_REQUEST");
        var unknown = post(vulnerablePort, json("😀\\\"\n"));
        assertThat(TestJson.MAPPER.readValue(unknown.body(), Object.class)).isEqualTo(Map.of("response", "DEMO_NO_SCENARIO"));
    }

    @Test void unsupportedMediaMethodsAndRoutesHaveClosedErrorsAndNoRemoteMutation() throws Exception {
        for (String type : List.of("text/plain", "application/problem+json", "application/json;charset=ISO-8859-1", "application/json;charset=UNKNOWN", "application/json;foo=bar")) {
            error(request(safePort, "POST", "/api/chat", json("hello"), "Content-Type", type), 415, "UNSUPPORTED_MEDIA_TYPE");
        }
        error(request(safePort, "POST", "/api/chat", json("hello")), 415, "UNSUPPORTED_MEDIA_TYPE");
        error(request(safePort, "POST", "/api/chat", json("hello"), "Content-Type", "application/json", "Content-Encoding", "gzip"), 415, "UNSUPPORTED_MEDIA_TYPE");
        error(request(safePort, "GET", "/api/chat", new byte[0]), 405, "METHOD_NOT_ALLOWED");
        error(request(safePort, "POST", "/health", new byte[0]), 405, "METHOD_NOT_ALLOWED");
        error(request(safePort, "OPTIONS", "/api/chat", new byte[0]), 405, "METHOD_NOT_ALLOWED");
        error(request(safePort, "GET", "/manifest/ground-truth-v1.json", new byte[0]), 404, "NOT_FOUND");
        error(request(safePort, "POST", "/api/chat?profile=VULNERABLE", json("hello"), "Content-Type", "application/json"), 400, "INVALID_REQUEST");
        var scenario = CatalogLoader.load().getFirst();
        var reply = request(safePort, "POST", "/api/chat", json(scenario.prompt()), "Content-Type", "application/json", "DEMO_PROFILE", "VULNERABLE", "Cookie", "profile=VULNERABLE");
        assertThat(TestJson.MAPPER.readValue(reply.body(), Object.class)).isEqualTo(Map.of("response", "DEMO_SAFE_LLM01_V1"));
    }

    @Test void byteBoundariesUseDecodedUtf8AndBoundedChunkedRead() throws Exception {
        assertThat(post(safePort, json("é".repeat(2048))).statusCode()).isEqualTo(200);
        error(post(safePort, json("é".repeat(2048) + "a")), 413, "REQUEST_TOO_LARGE");
        String body = "{\"message\":\"hola\"}";
        for (int length : List.of(8192, 8193)) {
            byte[] bytes = (body + " ".repeat(length - body.length())).getBytes(StandardCharsets.UTF_8);
            if (length == 8192) assertThat(post(safePort, bytes).statusCode()).isEqualTo(200);
            else error(post(safePort, bytes), 413, "REQUEST_TOO_LARGE");
            Thread.sleep(110);
            String reply = raw("POST /api/chat HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Type: application/json\r\nTransfer-Encoding: chunked\r\nConnection: close\r\n\r\n"
                    + Integer.toHexString(bytes.length) + "\r\n" + new String(bytes, StandardCharsets.UTF_8) + "\r\n0\r\n\r\n");
            assertThat(reply).startsWith("HTTP/1.1 " + (length == 8192 ? "200" : "413"));
            if (length == 8193) assertThat(reply).contains("{\"code\":\"REQUEST_TOO_LARGE\"}");
        }
        // El escape aumenta el body pero no los bytes del message decodificado.
        String escaped = "{\"message\":\"" + "\\u0061".repeat(1000) + "\"}";
        assertThat(post(safePort, escaped.getBytes(StandardCharsets.UTF_8)).statusCode()).isEqualTo(200);
    }

    @Test void oversizedHeadersAreRejectedByConnectorWithoutReflectingValues() throws Exception {
        String reply = raw("GET /health HTTP/1.1\r\nHost: 127.0.0.1\r\nX-Large: ORACLE_HEADER_ECHO" + "a".repeat(8192) + "\r\nConnection: close\r\n\r\n");
        assertThat(reply).matches("(?s)^HTTP/1.1 (400|431).*" ).doesNotContain("ORACLE_HEADER_ECHO", "Exception", "Request header");
    }

    @Test void immediatelyPipelinedRequestsRespectTheGlobalAdmissionInterval() throws Exception {
        Thread.sleep(110);
        String body = "{\"message\":\"hola\"}";
        String headers = "POST /api/chat HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Type: application/json\r\nContent-Length: " + body.length() + "\r\n";
        String reply = raw(headers + "\r\n" + body + headers + "Connection: close\r\n\r\n" + body);
        assertThat(reply).startsWith("HTTP/1.1 200").contains("HTTP/1.1 429", "{\"code\":\"RATE_LIMITED\"}");
    }

    @Test void idleBodyTimesOutAndInFlightRequestRejectsConcurrentChat() throws Exception {
        Thread.sleep(110);
        try (var socket = new Socket("127.0.0.1", safePort)) {
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(("POST /api/chat HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Type: application/json\r\nContent-Length: 100\r\nConnection: close\r\n\r\n{").getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            long started = System.nanoTime();
            Thread.sleep(150);
            var concurrent = post(safePort, json("hola"));
            error(concurrent, 429, "RATE_LIMITED");
            assertThat(request(safePort, "GET", "/health", new byte[0]).statusCode()).isEqualTo(200);
            String reply = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            long elapsed = Duration.ofNanos(System.nanoTime() - started).toMillis();
            assertThat(elapsed).isBetween(1400L, 4500L);
            assertThat(reply.isEmpty() || reply.startsWith("HTTP/1.1 408")).isTrue();
            if (!reply.isEmpty()) assertThat(reply).contains("{\"code\":\"REQUEST_TIMEOUT\"}");
        }
        // finally libera la compuerta incluso tras un timeout.
        assertThat(post(safePort, json("hola")).statusCode()).isEqualTo(200);
    }

    @Test void logsContainNoBodiesMarkersHeadersOrParsingCauses(CapturedOutput output) throws Exception {
        for (var scenario : CatalogLoader.load()) post(vulnerablePort, json(scenario.prompt()));
        post(vulnerablePort, "ORACLE_LOG_BODY_ECHO".getBytes(StandardCharsets.UTF_8));
        request(vulnerablePort, "GET", "/health", new byte[0], "X-Test", "ORACLE_LOG_HEADER_ECHO");
        assertThat(output.getAll()).doesNotContain("ORACLE_LOG_BODY_ECHO", "ORACLE_LOG_HEADER_ECHO", "DEMO_OVERRIDE_ACCEPTED_V1",
                "DEMO_CANARY_DO_NOT_USE_7F3A_V1", "DEMO_HIDDEN_CONTEXT_POLICY_V1", "DEMO_OUTPUT_EXTRA_V1", "JsonParseException");
        for (var scenario : CatalogLoader.load()) assertThat(output.getAll()).doesNotContain(scenario.prompt());
    }

    private String raw(String request) throws Exception {
        try (var socket = new Socket("127.0.0.1", safePort)) {
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
