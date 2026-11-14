package com.llmscannerseguridad.adapters.out.http.target;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.adapters.out.validation.target.*;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.application.target.EphemeralCredential;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.*;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class WorkflowCredentialsTest {
    private static final String HOST = "credentials.fixture.invalid";
    private static final String BEARER = "synthetic-bearer";
    private static final String COOKIE = "prefix-synthetic-bearer-suffix";
    private static final String CSRF = "synthetic-csrf";
    private static final TargetUrl ORIGIN = new TargetUrl("https://target-a.example");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final InteractionSession.Capture SESSION = new InteractionSession.Capture("sessionHandle",
            InteractionSession.Source.COOKIE, "session", List.of(), InteractionSession.Type.STRING, 128, true, true);
    private static final InteractionSession.Capture HEADER = new InteractionSession.Capture("csrfHandle",
            InteractionSession.Source.HEADER, "X-CSRF-Token", List.of(), InteractionSession.Type.STRING, 128, true, true);
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(strings = {"SSE", "JSONL"})
    void secureWorkflowForwardsDeclaredValuesAndRedactsTerminalEvidence(String protocol) throws Exception {
        var violations = new AtomicInteger();
        var requests = new AtomicInteger();
        var dnsCalls = new AtomicInteger();
        var decrypts = new AtomicInteger();
        var bindings = workflow(protocol);
        String media = protocol.equals("SSE") ? "text/event-stream" : "application/x-ndjson";
        try (var server = new ControlledHttpsServer(HOST, exchange -> {
            int step = requests.getAndIncrement() % 4;
            var headers = exchange.getRequestHeaders();
            var responseHeaders = exchange.getResponseHeaders();
            String input = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String path = exchange.getRequestURI().getPath();
            String[] paths = {"/login-bootstrap", "/api/chats", "/api/chats/chat-1/sessions", "/api/chats/chat-1/sessions/session-1/answer"};
            boolean valid = paths[step].equals(path) && ("Bearer " + BEARER).equals(headers.getFirst("Authorization"))
                    && (step == 0 ? !headers.containsKey("Cookie") && !headers.containsKey("X-CSRF-Token")
                        : ("session=" + COOKIE).equals(headers.getFirst("Cookie")) && CSRF.equals(headers.getFirst("X-CSRF-Token")))
                    && (step != 3 || media.equals(headers.getFirst("Accept")) && "synthetic-session-handle".equals(headers.getFirst("X-Session-Id"))
                        && input.contains(CSRF) && !headers.containsKey("X-Unbound"));
            if (!valid) {
                violations.incrementAndGet();
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            if (step == 0) {
                responseHeaders.add("Set-Cookie", "session=" + COOKIE + "; Path=/api; Secure; HttpOnly; SameSite=Strict");
                responseHeaders.add("Set-Cookie", "session=other-path; Path=/elsewhere; Secure");
                responseHeaders.add("Set-Cookie", "unbound=never-send; Path=/api; Secure");
                responseHeaders.add("X-CSRF-Token", CSRF);
            }
            String text = "safe " + COOKIE + " " + CSRF + " " + BEARER;
            String delta = JsonMapper.shared().writeValueAsString(Map.of("delta", text));
            String response = switch (step) {
                case 0 -> "{\"unbound\":\"unbound-secret\"}";
                case 1 -> "{\"chatId\":\"chat-1\"}";
                case 2 -> "{\"sessionId\":\"session-1\",\"sessionKey\":\"synthetic-session-handle\"}";
                default -> protocol.equals("SSE") ? "data: " + delta + "\n\ndata: [DONE]\n\n" : delta + "\n{\"done\":true}\n";
            };
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            responseHeaders.set("Content-Type", step == 3 ? media : "application/json");
            exchange.sendResponseHeaders(step == 3 ? 201 : 200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }); var dns = dns(dnsCalls); var logs = new WorkflowTargetTest.Logs()) {
            String origin = server.origin().replace("127.0.0.1", HOST);
            try (var transport = transport(demo(origin), dns, decrypts, server)) {
                var target = target(origin);
                var adapter = resolve(target, transport, bindings, List.of("X-CSRF-Token", "X-Session-Id", "X-Unbound"));
                for (int i = 0; i < 2; i++) {
                    var result = LegacyInvocationResultProjection.toLegacy(adapter.interact(target, "probe " + CSRF, budget()));
                    assertThat(result.errorReason()).isNull();
                    assertThat(result.httpStatus()).isEqualTo(201);
                    assertThat(result.responseText()).isEqualTo("safe [REDACTED_CREDENTIAL] [REDACTED_CREDENTIAL] [REDACTED_CREDENTIAL]");
                    assertThat(result.requestBody()).contains("[REDACTED_CREDENTIAL]").doesNotContain(CSRF);
                    assertThat(result.requestRedacted()).isTrue();
                    assertThat(result.responseRedacted()).isTrue();
                    assertThat(result.toString()).doesNotContain(COOKIE, CSRF, BEARER);
                }
                assertThat(requests).hasValue(8);
                assertThat(decrypts).hasValue(8);
                assertThat(violations).hasValue(0);
                assertThat(logs.messages()).contains("workflowStep=answer", "responseRedacted=true")
                        .doesNotContain(COOKIE, CSRF, BEARER, "session-1", "synthetic-session-handle", "unbound-secret");
            }
        }
    }

    @Test void capturesJsonHeaderAndCookiesWithPathAndExpiryScopes() {
        try (var session = new InteractionSession(ORIGIN)) {
            Instant beforeCapture = Instant.now();
            var json = new InteractionSession.Capture("jsonHandle", InteractionSession.Source.JSON, "$.csrf",
                    List.of("csrf"), InteractionSession.Type.STRING, 128, true, true);
            var response = reply("session=short; Path=/api; Max-Age=60",
                    "session=other; Path=/other; Expires=" + org.apache.hc.client5.http.utils.DateUtils.formatStandardDate(beforeCapture.plusSeconds(3600)));
            response.addHeader("X-CSRF-Token", CSRF);
            assertThatCode(() -> session.captureWorkflow(Map.of("csrf", "json-value"), response, List.of(SESSION, HEADER, json), ORIGIN))
                    .doesNotThrowAnyException();
            assertThat(session.credential(SESSION, url("/api/chat"), beforeCapture)).isEqualTo("short");
            assertThat(session.credential(SESSION, url("/other"), beforeCapture)).isEqualTo("other");
            assertThat(session.credential(HEADER, url("/api"), NOW)).isEqualTo(CSRF);
            assertThat(session.credential(json, url("/api"), NOW)).isEqualTo("json-value");
            assertUnavailable(() -> session.credential(SESSION, url("/api2"), beforeCapture));
            assertUnavailable(() -> session.credential(SESSION, url("/api"), Instant.now().plusSeconds(61)));
            assertUnavailable(() -> session.credential(HEADER, new TargetUrl("https://target-a.example:444/api"), NOW));
            assertUnavailable(() -> session.credential(SESSION, new TargetUrl("https://target-b.example/api"), NOW));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"session=x; Domain=target-a.example", "session=x; Max-Age=bad",
            "session=illegal,value", "session=x; Expires=invalid"})
    void rejectsMalformedOrDomainCookie(String cookie) {
        try (var session = new InteractionSession(ORIGIN)) {
            assertThatThrownBy(() -> session.captureWorkflow(Map.of(), reply(cookie), List.of(SESSION), ORIGIN))
                    .isInstanceOfSatisfying(InvocationFailure.class, error -> assertThat(error.reason).isEqualTo(ExecutionError.RESPONSE_FORMAT_ERROR));
        }
    }

    @Test void invalidatesDeletedCookiesAndRetainsRedactionUntilClose() {
        var session = new InteractionSession(ORIGIN);
        try (session) {
            session.captureWorkflow(Map.of(), reply("session=" + COOKIE + "; Path=/api; Secure; HttpOnly"), List.of(SESSION), ORIGIN);
            session.captureWorkflow(Map.of(), reply("session=; Path=/api; Max-Age=0"), List.of(), ORIGIN);
            assertUnavailable(() -> session.credential(SESSION, url("/api"), NOW));
            assertThat(session.redactor(BEARER).apply("before " + COOKIE + " after")).isEqualTo("before [REDACTED_CREDENTIAL] after");
        }
        assertUnavailable(() -> session.credential(SESSION, url("/api"), NOW));
        assertThat(session.redactor(null).apply(COOKIE)).isEqualTo(COOKIE);
    }

    @Test void appliesDefaultPathMaxAgePrecedenceAndSecure() {
        var source = java.net.URI.create("https://target-a.example/api/bootstrap");
        var cookie = WorkflowCookie.parse("session=ok; Secure; HttpOnly; SameSite=Lax; Max-Age=12; Expires=Wed, 01 Jan 2000 00:00:00 GMT",
                "session", source, NOW, 128);
        assertThat(cookie.path()).isEqualTo("/api");
        assertThat(cookie.httpOnly()).isTrue();
        assertThat(cookie.sameSite()).isEqualTo("Lax");
        assertThat(cookie.applies(source, NOW)).isTrue();
        assertThat(cookie.applies(source, NOW.plusSeconds(12))).isFalse();
        assertThat(cookie.applies(java.net.URI.create("http://target-a.example/api"), NOW)).isFalse();
        assertThatThrownBy(() -> WorkflowCookie.parse("session=ok; Secure", "session",
                java.net.URI.create("http://target-a.example/"), NOW, 128)).isInstanceOf(InvocationFailure.class);
    }

    @Test void rejectsAmbiguousApplicableCookiesAndDuplicatePaths() {
        try (var session = new InteractionSession(ORIGIN)) {
            session.captureWorkflow(Map.of(), reply("session=root; Path=/", "session=api; Path=/api"), List.of(SESSION), ORIGIN);
            assertUnavailable(() -> session.credential(SESSION, url("/api"), NOW));
        }
        try (var session = new InteractionSession(ORIGIN)) {
            assertThatThrownBy(() -> session.captureWorkflow(Map.of(),
                    reply("session=one; Path=/api", "session=two; Path=/api"), List.of(SESSION), ORIGIN))
                    .isInstanceOf(InvocationFailure.class);
        }
    }

    @Test void rejectsOversizeSensitiveCapture() {
        try (var session = new InteractionSession(ORIGIN)) {
            var response = reply();
            response.addHeader("X-CSRF-Token", "x".repeat(129));
            assertThatThrownBy(() -> session.captureWorkflow(Map.of(), response, List.of(HEADER), ORIGIN))
                    .isInstanceOfSatisfying(InvocationFailure.class, error -> assertThat(error.reason).isEqualTo(ExecutionError.RESPONSE_TOO_LARGE));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Accept", "Authorization", "Cookie", "Host", "Content-Length", "Transfer-Encoding", "Connection", "X-Unlisted", "X-Static"})
    void rejectsInvalidHeaderOrCollisionBeforeDnsAndDecryption(String header) throws Exception {
        var resolutions = new AtomicInteger();
        var decrypts = new AtomicInteger();
        try (var dns = dns(resolutions); var transport = transport(demo(ORIGIN.value()), dns, decrypts, null)) {
            var steps = workflow("SSE");
            steps.getLast().put("credentialBindings", List.of(binding("csrfHandle", "HEADER", header)));
            var target = target(ORIGIN.value());
            assertThatThrownBy(() -> resolve(target, transport, steps, header.equals("X-Unlisted") ? List.of() : List.of(header)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(resolutions).hasValue(0);
            assertThat(decrypts).hasValue(0);
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"false, HEADER", "false, URL", "false, BODY", "true, URL", "true, BODY"})
    void rejectsNonSensitiveCredentialSourcesAndSensitiveCredentialSlotsBeforeNetwork(boolean sensitive, String usage) throws Exception {
        var resolutions = new AtomicInteger();
        var decrypts = new AtomicInteger();
        try (var dns = dns(resolutions); var transport = transport(demo(ORIGIN.value()), dns, decrypts, null)) {
            var steps = workflow("SSE");
            steps.get(2).put("captures", List.of(capture("sessionId", "JSON", "$.sessionId", false),
                    capture("sessionKey", "JSON", "$.sessionKey", sensitive)));
            var terminal = steps.getLast();
            if (usage.equals("URL")) terminal.put("path", "/answer/{{sessionKey}}");
            if (usage.equals("BODY")) terminal.put("body", Map.of("prompt", "{{PROMPT}}", "value", "{{sessionKey}}"));
            var target = target(ORIGIN.value());
            assertThatThrownBy(() -> resolve(target, transport, steps, List.of("X-CSRF-Token", "X-Session-Id")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(resolutions).hasValue(0);
            assertThat(decrypts).hasValue(0);
        }
    }

    @Test void rejectsCrossOriginBeforeEitherCapturedValueOrBearerIsMaterialized() throws Exception {
        var resolutions = new AtomicInteger();
        var decrypts = new AtomicInteger();
        var policy = new TargetDestinationPolicy(Set.of("https://target-a.example", "https://target-b.example"), false, false, Set.of());
        try (var dns = dns(resolutions); var transport = transport(policy, dns, decrypts, null);
                var session = spy(new InteractionSession(ORIGIN))) {
            var steps = workflow("SSE");
            steps.getLast().put("path", "https://target-b.example/api");
            var target = target(ORIGIN.value());
            assertThatThrownBy(() -> resolve(target, transport, steps, List.of("X-CSRF-Token", "X-Session-Id")))
                    .isInstanceOf(IllegalArgumentException.class);
            var response = reply("session=" + COOKIE + "; Path=/api; Secure");
            response.addHeader("X-CSRF-Token", CSRF);
            session.captureWorkflow(Map.of(), response, List.of(SESSION, HEADER), ORIGIN);
            var result = transport.invokeWorkflow(target, "probe", budget(), "{}", "$.text", null,
                    new HttpTargetInvocationAdapter.SessionExchange("POST", new TargetUrl("https://target-b.example/api"), Long.MAX_VALUE, null, session),
                    new HttpTargetInvocationAdapter.WorkflowExchange(transport.workflowBudget(), Map.of(), new HttpTargetInvocationAdapter.StepMetrics(),
                            List.of(new CredentialBinding(SESSION, CredentialBinding.Destination.COOKIE, "session"),
                                    new CredentialBinding(HEADER, CredentialBinding.Destination.HEADER, "x-csrf-token"))));
            assertThat(result.errorReason()).isEqualTo(ExecutionError.TARGET_CONFIGURATION_INVALID);
            verify(session, never()).credential(any(), any(), any());
            assertThat(resolutions).hasValue(0);
            assertThat(decrypts).hasValue(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "path", "expired", "redirect"})
    void stopsBeforeSendingAnInvalidConsumerOrFollowingRedirect(String failure) throws Exception {
        var attempts = new AtomicInteger();
        try (var server = new ControlledHttpsServer(HOST, exchange -> {
            int step = attempts.getAndIncrement();
            if (step == 0) {
                if (!failure.equals("missing")) exchange.getResponseHeaders().add("X-CSRF-Token", CSRF);
                exchange.getResponseHeaders().add("Set-Cookie", "session=" + COOKIE
                        + (failure.equals("path") ? "; Path=/other" : "; Path=/api")
                        + (failure.equals("expired") ? "; Max-Age=0" : "") + "; Secure");
                byte[] bytes = "{\"unbound\":\"unbound-secret\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } else {
                exchange.getResponseHeaders().set("Location", "/api/next");
                exchange.sendResponseHeaders(302, -1);
            }
        }); var dns = dns(new AtomicInteger())) {
            String origin = server.origin().replace("127.0.0.1", HOST);
            try (var transport = transport(demo(origin), dns, new AtomicInteger(), server)) {
                var target = target(origin);
                var steps = workflow("SSE");
                if (failure.equals("missing")) {
                    var bootstrap = new HashMap<>(steps.getFirst());
                    var optional = new HashMap<>(capture("csrfHandle", "HEADER", "X-CSRF-Token", true));
                    optional.put("required", false);
                    bootstrap.put("captures", List.of(capture("sessionHandle", "COOKIE", "session", true), optional));
                    steps.set(0, bootstrap);
                }
                var result = resolve(target, transport, steps, List.of("X-CSRF-Token", "X-Session-Id"))
                        .interact(target, "probe", budget());
                assertThat(result.errorReason()).isEqualTo(failure.equals("redirect")
                        ? ExecutionError.HTTP_STATUS_ERROR : ExecutionError.CREDENTIAL_UNAVAILABLE);
                assertThat(result.responseText()).isNull();
                assertThat(attempts).hasValue(failure.equals("redirect") ? 2 : 1);
            }
        }
    }

    @Test void untrustedTlsPreventsHandleResolutionAndHttpSend() throws Exception {
        try (var server = new ControlledHttpsServer(HOST); var dns = dns(new AtomicInteger())) {
            String origin = server.origin().replace("127.0.0.1", HOST);
            var target = target(origin);
            try (var transport = transport(demo(origin), dns, new AtomicInteger(), null);
                    var session = spy(new InteractionSession(target.settings().url()))) {
                var response = reply("session=" + COOKIE + "; Path=/api; Secure");
                session.captureWorkflow(Map.of(), response, List.of(SESSION), target.settings().url());
                var step = new HttpTargetInvocationAdapter.SessionExchange("POST", new TargetUrl(origin + "/api"), Long.MAX_VALUE, null, session);
                var workflow = new HttpTargetInvocationAdapter.WorkflowExchange(transport.workflowBudget(), Map.of(),
                        new HttpTargetInvocationAdapter.StepMetrics(), List.of(new CredentialBinding(SESSION, CredentialBinding.Destination.COOKIE, "session")));
                assertThat(transport.invokeWorkflow(target, "probe", budget(), "{}", "$.text", null, step, workflow).errorReason())
                        .isEqualTo(ExecutionError.TARGET_CONNECTION_FAILED);
                verify(session, never()).credential(any(), any(), any());
                assertThat(server.received).hasValue(0);
            }
        }
    }

    @Test void reboundAddressIsRejectedBeforeReadingCapturedSecrets() throws Exception {
        var resolutions = new AtomicInteger();
        var decrypts = new AtomicInteger();
        var policy = new TargetDestinationPolicy(Set.of(ORIGIN.value()), false, false, Set.of());
        try (var dns = dns(resolutions); var transport = transport(policy, dns, decrypts, null);
                var session = spy(new InteractionSession(ORIGIN))) {
            session.captureWorkflow(Map.of(), reply("session=" + COOKIE + "; Path=/api; Secure"), List.of(SESSION), ORIGIN);
            var step = new HttpTargetInvocationAdapter.SessionExchange("POST", url("/api"), Long.MAX_VALUE, null, session);
            var workflow = new HttpTargetInvocationAdapter.WorkflowExchange(transport.workflowBudget(), Map.of(),
                    new HttpTargetInvocationAdapter.StepMetrics(), List.of(new CredentialBinding(SESSION, CredentialBinding.Destination.COOKIE, "session")));
            assertThat(transport.invokeWorkflow(target(ORIGIN.value()), "probe", budget(), "{}", "$.text", null, step, workflow).errorReason())
                    .isEqualTo(ExecutionError.TARGET_ADDRESS_BLOCKED);
            verify(session, never()).credential(any(), any(), any());
            assertThat(resolutions).hasValue(1);
            assertThat(decrypts).hasValue(0);
        }
    }

    @Test void cancellationAfterCaptureNeverLeaksHandlesToNextInteraction() throws Exception {
        var captures = new AtomicInteger();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        try (var server = new ControlledHttpsServer(HOST, exchange -> {
            if (exchange.getRequestURI().getPath().equals("/login-bootstrap")) {
                if (captures.incrementAndGet() == 1) {
                    exchange.getResponseHeaders().add("X-CSRF-Token", CSRF);
                    exchange.getResponseHeaders().add("Set-Cookie", "session=" + COOKIE + "; Path=/api; Secure");
                }
                byte[] body = "{\"unbound\":\"unbound-secret\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                entered.countDown();
                try { release.await(5, java.util.concurrent.TimeUnit.SECONDS); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                exchange.sendResponseHeaders(503, -1);
            }
        }); var dns = dns(new AtomicInteger())) {
            String origin = server.origin().replace("127.0.0.1", HOST);
            try (var transport = transport(demo(origin), dns, new AtomicInteger(), server)) {
                var target = target(origin);
                var adapter = resolve(target, transport, workflow("SSE"), List.of("X-CSRF-Token", "X-Session-Id"));
                var result = new java.util.concurrent.FutureTask<>(() -> adapter.interact(target, "probe", budget()));
                var caller = Thread.ofPlatform().start(result);
                try {
                    assertThat(entered.await(6, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                    caller.interrupt();
                    assertThat(result.get(5, java.util.concurrent.TimeUnit.SECONDS).errorReason()).isEqualTo(ExecutionError.EXECUTION_INTERRUPTED);
                } finally {
                    release.countDown();
                    caller.interrupt();
                }
                assertThat(adapter.interact(target, "probe", budget()).errorReason()).isEqualTo(ExecutionError.RESPONSE_PATH_NOT_FOUND);
                assertThat(server.received).hasValue(3);
            }
        }
    }

    private static BasicClassicHttpResponse reply(String... cookies) {
        var response = new BasicClassicHttpResponse(200);
        for (String cookie : cookies) response.addHeader("Set-Cookie", cookie);
        return response;
    }

    private static TargetUrl url(String path) { return new TargetUrl(ORIGIN.value() + path); }
    private static ScanBudget budget() { return new ScanBudget(Instant.now(), 30000, Clock.systemUTC(), System::nanoTime); }
    private static void assertUnavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(InvocationFailure.class,
                error -> assertThat(error.reason).isEqualTo(ExecutionError.CREDENTIAL_UNAVAILABLE));
    }

    private static List<Map<String, Object>> workflow(String protocol) {
        var steps = WorkflowTargetTest.steps(protocol);
        steps.getFirst().put("captures", List.of(capture("chatId", "JSON", "$.chatId", false)));
        steps.get(1).put("captures", List.of(capture("sessionId", "JSON", "$.sessionId", false),
                capture("sessionKey", "JSON", "$.sessionKey", true)));
        var bootstrap = Map.<String, Object>of("id", "bootstrap", "method", "POST", "path", "/login-bootstrap", "body", Map.of(),
                "dependsOn", List.of(), "response", Map.of("type", "JSON"),
                "captures", List.of(capture("sessionHandle", "COOKIE", "session", true), capture("csrfHandle", "HEADER", "X-CSRF-Token", true),
                        capture("unboundHandle", "JSON", "$.unbound", true)));
        for (var step : steps) {
            var dependencies = new ArrayList<Object>((List<?>) step.get("dependsOn"));
            dependencies.addFirst("bootstrap");
            step.put("dependsOn", dependencies);
            var bindings = new ArrayList<>(List.of(binding("sessionHandle", "COOKIE", "session"), binding("csrfHandle", "HEADER", "X-CSRF-Token")));
            if (step == steps.getLast()) bindings.add(binding("sessionKey", "HEADER", "X-Session-Id"));
            step.put("credentialBindings", bindings);
        }
        steps.addFirst(bootstrap);
        return steps;
    }

    private static Map<String, Object> capture(String name, String source, String selector, boolean sensitive) {
        return SessionTargetTest.capture(name, source, selector, true, sensitive, 128);
    }

    private static Map<String, String> binding(String capture, String destination, String name) {
        return Map.of("capture", capture, "destination", destination, "name", name);
    }

    private TargetAdapter resolve(TargetConfiguration target, HttpTargetInvocationAdapter transport,
            List<Map<String, Object>> steps, List<String> allowed) throws Exception {
        var entry = Map.of("projectId", target.projectId(), "targetConfigurationId", target.id(), "targetUpdatedAt",
                target.updatedAt().toString(), "workflowId", "credentials", "revision", 1, "steps", steps, "allowedDynamicHeaders", allowed);
        var file = directory.resolve("credentials.json");
        Files.writeString(file, JsonMapper.shared().writeValueAsString(Map.of("schemaVersion", 1, "bindings", List.of(entry))));
        return new LocalWorkflowBindings(file, transport).resolve(target);
    }

    private static TargetConfiguration target(String origin) {
        Instant at = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return new TargetConfiguration(UUID.randomUUID(), UUID.randomUUID(), new TargetSettings(null, new TargetUrl(origin + "/unused"),
                "{\"prompt\":\"{{PROMPT}}\"}", "$.text", Map.of("X-Static", "fixture"), AuthType.BEARER_TOKEN, null, 16000),
                new EncryptedCredential(new byte[]{1}), at, at);
    }

    private static TargetDestinationPolicy demo(String origin) { return new TargetDestinationPolicy(Set.of(), true, true, Set.of(origin)); }
    private static BoundedDnsResolver dns(AtomicInteger counter) {
        return new BoundedDnsResolver(host -> {
            counter.incrementAndGet();
            return new InetAddress[] {InetAddress.getLoopbackAddress()};
        });
    }

    private static HttpTargetInvocationAdapter transport(TargetDestinationPolicy policy, BoundedDnsResolver dns,
            AtomicInteger decrypts, ControlledHttpsServer server) {
        var crypto = (com.llmscannerseguridad.application.target.CredentialDecryptionPort) ignored -> {
            decrypts.incrementAndGet();
            return new EphemeralCredential(BEARER.getBytes(StandardCharsets.UTF_8));
        };
        var json = new TargetJsonProcessor(new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of())));
        return server == null ? new HttpTargetInvocationAdapter(policy, dns, json, crypto, ExecutionLimits.defaults())
                : new HttpTargetInvocationAdapter(policy, dns, json, crypto, ExecutionLimits.defaults(),
                    SSLConnectionSocketFactoryBuilder.create().setSslContext(server.trustedContext).build());
    }
}
