package com.llmscannerseguridad.adapters.out.http.target;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.adapters.out.validation.target.*;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.*;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class SessionTargetTest {
    static final String TEMPLATE = """
            {"message":"{{PROMPT}}","chat":"{{chatId}}","session":"{{sessionId}}"}
            """;
    static final String BOOTSTRAP = """
            {"chatId":"chat-123","session":{"id":"session-456"},"privateValue":"synthetic-sensitive"}
            """;
    @TempDir Path dir;

    @Test void bootstrapAndPostResolveBothSlotsRedactCapturesAndNeverForwardHandles() throws Exception {
        try (var fixture = new Fixture(BOOTSTRAP, TEMPLATE)) {
            var adapter = fixture.resolve(captures(), "/chats/{{chatId}}/sessions/{{sessionId}}", "GET");
            var result = adapter.interact(fixture.target, "{{chatId}}", budget());
            assertThat(result.errorReason()).isNull();
            assertThat(fixture.bootstrapMethod).isEqualTo("GET");
            assertThat(fixture.bootstrapRequest).isEmpty();
            assertThat(fixture.path).isEqualTo("/chats/chat-123/sessions/session-456");
            assertThat(new StrictTargetJson().read(fixture.body)).isEqualTo(Map.of("chat", "chat-123", "session", "session-456", "message", "{{chatId}}"));
            assertThat(fixture.forwarded).isFalse();
            assertThat(fixture.requests).hasValue(2);
            assertThat(result.responseText()).isEqualTo("[REDACTED_CREDENTIAL] [REDACTED_CREDENTIAL] [REDACTED_CREDENTIAL]");
            assertThat(result.responseRedacted()).isTrue();
            var projected = LegacyInvocationResultProjection.toLegacy(result);
            assertThat(projected.httpStatus()).isEqualTo(201);
            assertThat(projected.requestBody()).doesNotContain("synthetic-sensitive", "synthetic-cookie", "synthetic-header");
        }
    }

    @Test void supportsPostBootstrapAndEncodesPathOnce() throws Exception {
        try (var fixture = new Fixture(BOOTSTRAP.replace("chat-123", "chat é"), TEMPLATE)) {
            var result = fixture.resolve(captures(), "/chats/{{chatId}}", "POST").interact(fixture.target, "test", budget());
            assertThat(result.errorReason()).isNull();
            assertThat(fixture.bootstrapMethod).isEqualTo("POST");
            assertThat(fixture.bootstrapRequest).isEqualTo("{\"start\":true}");
            assertThat(fixture.path).isEqualToIgnoringCase("/chats/chat%20%C3%A9");
        }
    }

    @Test void overlappingStaticCredentialAndCapturedSecretAreRedactedBeforeEvaluation() throws Exception {
        String sensitive = "prefix-abc123-suffix";
        String marker = "[REDACTED_CREDENTIAL]";
        String observed = "SYNTHETIC_CANARY_7 " + sensitive + " abc123 " + sensitive;
        String sanitized = "SYNTHETIC_CANARY_7 " + marker + " " + marker + " " + marker;
        try (var fixture = new Fixture(BOOTSTRAP.replace("synthetic-sensitive", sensitive), TEMPLATE, 6000, "abc123")) {
            fixture.finalResponse = JsonMapper.shared().writeValueAsString(Map.of("text", observed));
            var result = LegacyInvocationResultProjection.toLegacy(
                    fixture.resolve(captures(), null, "GET").interact(fixture.target, sensitive, budget()));
            assertThat(result.errorReason()).isNull();
            assertThat(result.responseText()).isEqualTo(sanitized);
            assertThat(result.requestBody()).doesNotContain("prefix-", "abc123", "-suffix").contains(marker);
            assertThat(result.requestRedacted()).isTrue();
            assertThat(result.responseRedacted()).isTrue();

            var test = com.llmscannerseguridad.adapters.out.evaluation.EvaluationFixtures.test();
            var settings = com.llmscannerseguridad.adapters.out.evaluation.EvaluationFixtures.settings();
            var messages = new com.llmscannerseguridad.adapters.out.evaluation.EvaluatorMessages();
            var projector = new com.llmscannerseguridad.adapters.out.evaluation.EvaluationEvidenceProjector(settings, messages, Clock.systemUTC());
            var criteria = new com.llmscannerseguridad.adapters.out.evaluation.ResourceEvaluationCriteriaCatalog().find(test);
            var context = com.llmscannerseguridad.adapters.out.evaluation.EvaluationFixtures.context("LLM02");
            var entry = new com.llmscannerseguridad.application.evaluation.EvaluationScenarioPort.Entry(test.code(), 1, context, List.of(sanitized));
            var scenario = new com.llmscannerseguridad.application.evaluation.EvaluationScenarioPort.Scenario(
                    "fixture", "1", "hash", true, "OPENAI", Map.of(test.code(), entry));
            var at = fixture.target.updatedAt();
            var execution = com.llmscannerseguridad.domain.execution.TestExecution.pending(
                    UUID.randomUUID(), UUID.randomUUID(), test.id(), fixture.target.id(), at).start(at, at)
                    .finish(at, result.httpStatus(), result.latencyMs(), null, result.requestBody(), result.responseText(),
                            result.requestRedacted(), result.responseRedacted());
            var id = UUID.randomUUID();
            var pending = com.llmscannerseguridad.domain.evaluation.EvaluationResult.pending(id, execution.id(), execution.scanId(),
                    test.id(), test.category(), at, projector.configuration(id, criteria, scenario));
            var prepared = projector.prepare(pending, execution, test);
            assertThat(prepared.requestLlm()).isTrue();
            assertThat(prepared.input().evidenceText()).isEqualTo(sanitized);
            assertThat(prepared.result().evidence().projection()).contains(sanitized).doesNotContain("prefix-", "abc123", "-suffix");
            assertThat(messages.request(prepared.input(), settings)).contains(sanitized).doesNotContain("prefix-", "abc123", "-suffix");
        }
    }

    @Test void sessionRedactorMergesPartialOverlapsRegardlessOfCredentialOrderAndKeepsSafeText() {
        var origin = new TargetUrl("https://fixture.invalid");
        for (var pair : List.of(List.of("abcdef", "defghi"), List.of("defghi", "abcdef"),
                List.of("abc123", "prefix-abc123-suffix"), List.of("prefix-abc123-suffix", "abc123"))) {
            try (var session = new InteractionSession(origin)) {
                var declaration = new InteractionSession.Capture("handle", InteractionSession.Source.JSON, "$.value", List.of("value"),
                        InteractionSession.Type.STRING, 128, true, true);
                session.capture(Map.of("value", pair.getLast()), new BasicClassicHttpResponse(200), List.of(declaration));
                var redact = session.redactor(pair.getFirst());
                String marker = "[REDACTED_CREDENTIAL]";
                String combined = pair.contains("abcdef") ? "abcdefghi" : "prefix-abc123-suffix";
                assertThat(redact.apply(combined + " " + pair.getFirst() + " " + pair.getLast() + " " + combined))
                        .isEqualTo(String.join(" ", Collections.nCopies(4, marker)));
                assertThat(redact.apply("Texto sin datos sensibles")).isEqualTo("Texto sin datos sensibles");
            }
        }
    }

    @Test void rejectsCanonicalDuplicateJsonSelectorsBeforeBootstrapButAllowsDistinctPaths() throws Exception {
        try (var fixture = new Fixture("{\"chatId\":\"chat-123\",\"sessionId\":\"session-456\"}", TEMPLATE)) {
            for (String[] pair : List.of(new String[]{"$.chatId", "$[\"chatId\"]"},
                    new String[]{"$.chatId", "$[\"\\u0063hatId\"]"},
                    new String[]{"$.items[0].id", "$[\"items\"][0][\"id\"]"},
                    new String[]{"$.chatId", "$.\"chatId\""})) {
                assertThatThrownBy(() -> fixture.resolve(List.of(
                        capture("chatId", "JSON", pair[0], true, false, 128),
                        capture("sessionId", "JSON", pair[1], true, false, 128)), null, "GET"))
                        .hasMessage("Binding de sesión no válido.");
            }
            assertThat(fixture.requests).hasValue(0);
            var adapter = fixture.resolve(List.of(capture("chatId", "JSON", "$.chatId", true, false, 128),
                    capture("sessionId", "JSON", "$.sessionId", true, false, 128)), null, "GET");
            assertThat(adapter.interact(fixture.target, "test", budget()).errorReason()).isNull();
            assertThat(fixture.requests).hasValue(2);
        }
    }

    @Test void finalPostCannotGetAnotherDeadlineWhenQuotaWaitConsumesTheSharedTimeout() throws Exception {
        try (var fixture = new Fixture(BOOTSTRAP, TEMPLATE, 1000)) {
            var result = fixture.resolve(captures(), null, "GET").interact(fixture.target, "test", budget());
            assertFailure(fixture, result, ExecutionError.TARGET_TIMEOUT);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"..", "%2f", "other/host", "other\\host", "other?query", "other#fragment"})
    void pathTraversalAndDelimitersNeverSendDependentPost(String invalid) throws Exception {
        String body = JsonMapper.shared().writeValueAsString(Map.of("chatId", invalid, "session", Map.of("id", "session-456")));
        try (var fixture = new Fixture(body, TEMPLATE)) {
            var result = fixture.resolve(captures().subList(0, 2), "/chats/{{chatId}}", "GET").interact(fixture.target, "test", budget());
            assertFailure(fixture, result, ExecutionError.TARGET_CONFIGURATION_INVALID);
        }
    }

    @Test void missingExtractionAndMissingOptionalSlotBothAbortWithoutPost() throws Exception {
        try (var fixture = new Fixture("{\"chatId\":\"chat-123\"}", TEMPLATE)) {
            assertFailure(fixture, fixture.resolve(captures().subList(0, 2), null, "GET").interact(fixture.target, "test", budget()),
                    ExecutionError.RESPONSE_PATH_NOT_FOUND);
        }
        try (var fixture = new Fixture("{\"chatId\":\"chat-123\"}", TEMPLATE)) {
            var declarations = List.of(captures().getFirst(), capture("sessionId", "JSON", "$.session.id", false, false, 4096));
            assertFailure(fixture, fixture.resolve(declarations, null, "GET").interact(fixture.target, "test", budget()),
                    ExecutionError.TARGET_CONFIGURATION_INVALID);
        }
    }

    @Test void invalidScalarAndOversizeAbortWithoutPost() throws Exception {
        for (Object invalid : List.of(Map.of("nested", true), 123, "é".repeat(2049))) {
            String body = JsonMapper.shared().writeValueAsString(Map.of("chatId", invalid, "session", Map.of("id", "session-456")));
            try (var fixture = new Fixture(body, TEMPLATE)) {
                assertFailure(fixture, fixture.resolve(captures().subList(0, 2), null, "GET").interact(fixture.target, "test", budget()),
                        invalid instanceof String ? ExecutionError.RESPONSE_TOO_LARGE : ExecutionError.RESPONSE_FORMAT_ERROR);
            }
        }
    }

    @Test void rejectsReservedNamesDuplicatesSensitiveSlotsAndForeignPathsBeforeBootstrap() throws Exception {
        try (var fixture = new Fixture(BOOTSTRAP, TEMPLATE)) {
            for (String name : List.of("Authorization", "apiKey", "scanId", "ChatId")) {
                var declarations = new ArrayList<>(captures());
                declarations.add(capture(name, "JSON", "$.other", true, false, 10));
                assertThatThrownBy(() -> fixture.resolve(declarations, null, "GET")).hasMessage("Binding de sesión no válido.");
            }
            for (String path : List.of("//other.invalid", "/{{headerHandle}}", "/{{cookieHandle}}", "/prefix-{{chatId}}", "/{{unknown}}")) {
                assertThatThrownBy(() -> fixture.resolve(captures(), path, "GET")).hasMessage("Binding de sesión no válido.");
            }
            assertThat(fixture.requests).hasValue(0);
        }
    }

    @Test void capturesHaveOriginAndSensitiveCookieAttributesAndAreClearedOnClose() {
        var origin = new TargetUrl("https://fixture.invalid");
        var session = new InteractionSession(origin);
        var response = new BasicClassicHttpResponse(200);
        response.addHeader("Set-Cookie", "fixtureId=synthetic-cookie; Path=/fixture; Secure; HttpOnly; SameSite=Lax");
        var declarations = List.of(
                new InteractionSession.Capture("chatId", InteractionSession.Source.JSON, "$.chatId", List.of("chatId"),
                        InteractionSession.Type.STRING, 40, true, false),
                new InteractionSession.Capture("handle", InteractionSession.Source.COOKIE, "fixtureId", List.of(),
                        InteractionSession.Type.STRING, 40, true, true));
        try (session; var other = new InteractionSession(origin)) {
            session.capture(Map.of("chatId", "chat-123"), response, declarations);
            var cookie = session.value("handle");
            assertThat(cookie.origin()).isEqualTo(origin.origin());
            assertThat(cookie.source()).isEqualTo(InteractionSession.Source.COOKIE);
            assertThat(cookie.sensitive()).isTrue();
            assertThat(cookie.cookieAttributes()).containsEntry("path", "/fixture").containsKeys("secure", "httponly", "samesite");
            assertThat(cookie.toString()).doesNotContain("synthetic-cookie");
            assertThat(session.value("chatId").origin()).isEqualTo(origin.origin());
            assertThat(session.identifier("chatId", origin)).isEqualTo("chat-123");
            assertThatThrownBy(() -> session.identifier("chatId", new TargetUrl("https://other.invalid"))).isInstanceOf(InvocationFailure.class);
            assertThatThrownBy(() -> session.identifier("handle", origin)).isInstanceOf(InvocationFailure.class);
            assertThatThrownBy(() -> other.identifier("chatId", origin)).isInstanceOf(InvocationFailure.class);
        }
        assertThat(session.value("chatId")).isNull();
        assertThat(session.value("handle")).isNull();
        assertThatThrownBy(() -> session.identifier("chatId", origin)).isInstanceOf(InvocationFailure.class);
    }

    @Test void failedCaptureAndCancellationDoNotReuseFirstInteractionVariables() throws Exception {
        try (var fixture = new Fixture(BOOTSTRAP, TEMPLATE)) {
            var adapter = fixture.resolve(captures(), null, "GET");
            assertThat(adapter.interact(fixture.target, "first", budget()).errorReason()).isNull();
            fixture.bootstrap = "{\"chatId\":\"another\"}";
            assertThat(adapter.interact(fixture.target, "second", budget()).errorReason()).isEqualTo(ExecutionError.RESPONSE_PATH_NOT_FOUND);
            Thread.currentThread().interrupt();
            try {
                assertThat(adapter.interact(fixture.target, "cancelled", budget()).errorReason()).isEqualTo(ExecutionError.EXECUTION_INTERRUPTED);
                assertThat(Thread.currentThread().isInterrupted()).isTrue();
            } finally { Thread.interrupted(); }
            assertThat(fixture.requests).hasValue(3);
        }
    }

    @Test void duplicateHeadersCookiesAndJsonFieldsAbortBeforePost() throws Exception {
        for (String source : List.of("HEADER", "COOKIE", "JSON")) {
            try (var fixture = new Fixture(source.equals("JSON")
                    ? BOOTSTRAP.replace("\"chatId\":\"chat-123\"", "\"chatId\":\"chat-123\",\"chatId\":\"another\"")
                    : BOOTSTRAP, TEMPLATE)) {
                fixture.duplicate = source;
                assertFailure(fixture, fixture.resolve(captures(), null, "GET").interact(fixture.target, "test", budget()),
                        source.equals("JSON") ? ExecutionError.RESPONSE_INVALID_JSON : ExecutionError.RESPONSE_FORMAT_ERROR);
            }
        }
    }

    @Test void cancelledBootstrapClosesInteractionAndNeverSendsPost() throws Exception {
        try (var fixture = new Fixture(BOOTSTRAP, TEMPLATE);
                var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            fixture.holdBootstrap = true;
            var adapter = fixture.resolve(captures(), null, "GET");
            var thread = new java.util.concurrent.atomic.AtomicReference<Thread>();
            var result = worker.submit(() -> {
                thread.set(Thread.currentThread());
                return adapter.interact(fixture.target, "test", budget());
            });
            try {
                assertThat(fixture.started.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                thread.get().interrupt();
                assertFailure(fixture, result.get(3, java.util.concurrent.TimeUnit.SECONDS), ExecutionError.EXECUTION_INTERRUPTED);
            } finally {
                fixture.release.countDown();
            }
        }
    }

    private static void assertFailure(Fixture fixture, NormalizedInteraction result, ExecutionError error) {
        assertThat(result.errorReason()).isEqualTo(error);
        assertThat(result.responseText()).isNull();
        assertThat(LegacyInvocationResultProjection.toLegacy(result).requestBody()).isNull();
        assertThat(fixture.requests).hasValue(1);
    }

    private static ScanBudget budget() { return new ScanBudget(Instant.now(), 300000, Clock.systemUTC(), System::nanoTime); }

    static List<Map<String, Object>> captures() {
        return List.of(capture("chatId", "JSON", "$.chatId", true, false, 4096),
                capture("sessionId", "JSON", "$.session.id", true, false, 4096),
                capture("privateHandle", "JSON", "$.privateValue", true, true, 4096),
                capture("headerHandle", "HEADER", "X-Fixture", true, true, 4096),
                capture("cookieHandle", "COOKIE", "fixtureId", true, true, 4096));
    }

    static Map<String, Object> capture(String name, String source, String selector, boolean required, boolean sensitive, int max) {
        return Map.of("name", name, "source", source, "selector", selector, "type", "STRING", "required", required, "sensitive", sensitive, "maxBytes", max);
    }

    final class Fixture implements AutoCloseable {
        final HttpServer server;
        final BoundedDnsResolver dns = new BoundedDnsResolver(InetAddress::getAllByName);
        final HttpTargetInvocationAdapter transport;
        final TargetConfiguration target;
        final AtomicInteger requests = new AtomicInteger();
        volatile String bootstrap, bootstrapMethod, bootstrapRequest, path, body;
        volatile String finalResponse = "{\"text\":\"synthetic-sensitive synthetic-header synthetic-cookie\"}";
        volatile String duplicate;
        volatile boolean holdBootstrap;
        volatile boolean forwarded;
        final java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);

        Fixture(String bootstrap, String template) throws Exception {
            this(bootstrap, template, 6000);
        }

        Fixture(String bootstrap, String template, int timeoutMs) throws Exception {
            this(bootstrap, template, timeoutMs, null);
        }

        Fixture(String bootstrap, String template, int timeoutMs, String credential) throws Exception {
            this.bootstrap = bootstrap;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            server.createContext("/", exchange -> {
                try (exchange) {
                    requests.incrementAndGet();
                    String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    boolean init = exchange.getRequestURI().getPath().equals("/bootstrap");
                    String response;
                    if (init) {
                        bootstrapMethod = exchange.getRequestMethod();
                        bootstrapRequest = request;
                        exchange.getResponseHeaders().add("X-Fixture", "synthetic-header");
                        exchange.getResponseHeaders().add("Set-Cookie", "fixtureId=synthetic-cookie; Path=/; HttpOnly");
                        if ("HEADER".equals(duplicate)) exchange.getResponseHeaders().add("X-Fixture", "duplicate");
                        if ("COOKIE".equals(duplicate)) exchange.getResponseHeaders().add("Set-Cookie", "fixtureId=duplicate");
                        response = this.bootstrap;
                    } else {
                        path = exchange.getRequestURI().getRawPath();
                        body = request;
                        forwarded = exchange.getRequestHeaders().containsKey("Cookie") || exchange.getRequestHeaders().containsKey("X-Fixture");
                        response = finalResponse;
                    }
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(init ? 200 : 201, bytes.length);
                    if (init && holdBootstrap) {
                        exchange.getResponseBody().write(bytes[0]);
                        exchange.getResponseBody().flush();
                        started.countDown();
                        try { release.await(5, java.util.concurrent.TimeUnit.SECONDS); }
                        catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                        return;
                    }
                    exchange.getResponseBody().write(bytes);
                }
            });
            server.start();
            transport = new HttpTargetInvocationAdapter(new TargetDestinationPolicy(Set.of(), true, true, Set.of(origin)), dns,
                    new TargetJsonProcessor(new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of()))),
                    ignored -> new com.llmscannerseguridad.application.target.EphemeralCredential(
                            Objects.requireNonNull(credential).getBytes(StandardCharsets.UTF_8)), ExecutionLimits.defaults());
            var now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            target = new TargetConfiguration(UUID.randomUUID(), UUID.randomUUID(), new TargetSettings(null, new TargetUrl(origin + "/answer"),
                    template, "$.text", Map.of(), credential == null ? AuthType.NONE : AuthType.BEARER_TOKEN, null, timeoutMs),
                    credential == null ? null : new EncryptedCredential(new byte[] {1}), now, now);
        }

        TargetAdapter resolve(List<Map<String, Object>> captures, String path, String method) throws Exception {
            var entry = new HashMap<String, Object>(Map.of("projectId", target.projectId(), "targetConfigurationId", target.id(),
                    "targetUpdatedAt", target.updatedAt().toString(), "captures", captures,
                    "bootstrap", method.equals("POST") ? Map.of("method", method, "path", "/bootstrap", "body", Map.of("start", true))
                            : Map.of("method", method, "path", "/bootstrap")));
            if (path != null) entry.put("promptPath", path);
            var file = dir.resolve("session.json");
            Files.writeString(file, JsonMapper.shared().writeValueAsString(Map.of("schemaVersion", 1, "bindings", List.of(entry))));
            return new LocalSessionBindings(file, transport).resolve(target);
        }

        @Override public void close() { server.stop(0); transport.close(); dns.close(); }
    }
}
