package com.llmscannerseguridad.adapters.out.http.target;

import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.adapters.out.validation.target.*;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.application.target.EphemeralCredential;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

class WorkflowTargetTest {
    private static final String FIXTURE_CREDENTIAL = "synthetic-bearer";
    private static final String PRIVATE = "prefix-synthetic-bearer-suffix";
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(strings = {"SSE", "JSONL", "JSON"})
    void reconstructsTerminalAndReusesOnlyCurrentInteractionVariables(String type) throws Exception {
        try (var fixture = new Fixture(type, 12000, 262144, 65536); var logs = new Logs()) {
            var adapter = fixture.resolve(steps(type));
            var result = legacy(adapter.interact(fixture.target, "{{chatId}}", budget(30000)));
            assertThat(result.errorReason()).isNull();
            assertThat(result.httpStatus()).isEqualTo(201);
            assertThat(result.responseText()).isEqualTo("respuesta final é [REDACTED_CREDENTIAL]");
            assertThat(result.responseRedacted()).isTrue();
            assertThat(result.requestBody()).isEqualTo("{\"prompt\":\"{{chatId}}\"}");
            assertThat(fixture.paths).containsExactly("/api/chats", "/api/chats/chat-1/sessions",
                    "/api/chats/chat-1/sessions/session-1/answer");
            assertThat(fixture.decrypts).hasValue(3);
            assertThat(fixture.resolutions).hasValue(3);
            assertThat(fixture.badRequests).hasValue(0);
            assertThat(logs.messages()).contains("workflowStep=answer ordinal=3 state=COMPLETED", "httpStatus=201",
                    "responseRedacted=true").doesNotContain(PRIVATE, FIXTURE_CREDENTIAL, "chat-1", "session-1", "{{chatId}}");
            assertThat(adapter.interact(fixture.target, "next", budget(30000)).errorReason()).isNull();
            assertThat(fixture.paths.subList(3, 6)).containsExactly("/api/chats", "/api/chats/chat-2/sessions",
                    "/api/chats/chat-2/sessions/session-2/answer");
        }
    }

    @Test void rejectsCrossOriginEvenWhenBothHostsAreAllowed() throws Exception {
        var allow = Set.of("https://target-a.example", "https://target-b.example");
        var policy = new TargetDestinationPolicy(allow, false, false, Set.of());
        assertThat(policy.authorize(new TargetUrl("https://target-a.example"))).isFalse();
        assertThat(policy.authorize(new TargetUrl("https://target-b.example"))).isFalse();
        var resolutions = new AtomicInteger();
        var decrypts = new AtomicInteger();
        var target = target("https://target-a.example", 12000);
        try (var dns = new BoundedDnsResolver(host -> {
            resolutions.incrementAndGet();
            throw new UnknownHostException();
        }); var transport = transport(policy, dns, decrypts, ExecutionLimits.defaults());
                var session = new InteractionSession(target.settings().url()); var logs = new Logs()) {
            var steps = steps("SSE");
            steps.get(1).put("path", "//target-b.example/path");
            assertThatThrownBy(() -> bindings(target, transport, steps, 1).resolve(target))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(logs.messages()).contains("workflowStep=session ordinal=2 error=TARGET_CONFIGURATION_INVALID")
                    .doesNotContain("target-a", "target-b", "/path", FIXTURE_CREDENTIAL);
            for (String destination : List.of("http://target-a.example:443/path", "https://target-b.example/path", "https://target-a.example:444/path")) {
                var result = transport.invokeWorkflow(target, "probe", budget(30000), "{}", "$.text", null,
                        new HttpTargetInvocationAdapter.SessionExchange("POST", new TargetUrl(destination), Long.MAX_VALUE, null, session),
                        new HttpTargetInvocationAdapter.WorkflowExchange(transport.workflowBudget(), Map.of(), new HttpTargetInvocationAdapter.StepMetrics(), List.of()));
                assertThat(result.errorReason()).isEqualTo(ExecutionError.TARGET_CONFIGURATION_INVALID);
                assertThat(result.responseText()).isNull();
            }
            assertThat(resolutions).hasValue(0);
            assertThat(decrypts).hasValue(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"duplicate", "invalid-id", "future", "no-prompt", "intermediate-SSE", "intermediate-JSONL",
            "version", "too-many", "sensitive-guard", "accept", "no-dependency"})
    void rejectsInvalidPlanBeforeAnyExchange(String invalid) throws Exception {
        try (var fixture = new Fixture("SSE", 12000, 262144, 65536)) {
            var steps = steps("SSE");
            switch (invalid) {
                case "duplicate" -> steps.get(1).put("id", "chat");
                case "invalid-id" -> steps.get(1).put("id", "bad step");
                case "future" -> steps.getFirst().put("dependsOn", List.of("answer"));
                case "no-prompt" -> steps.getLast().put("body", Map.of());
                case "intermediate-SSE", "intermediate-JSONL" -> steps.get(1).put("response", response(invalid.substring(13)));
                case "too-many" -> steps.addAll(Collections.nCopies(4, steps.getFirst()));
                case "sensitive-guard" -> steps.get(1).put("guard", Map.of("name", "privateHandle", "operator", "exists"));
                case "accept" -> steps.get(1).put("headers", Map.of("aCcEpT", "text/plain"));
                case "no-dependency" -> steps.get(1).put("dependsOn", List.of());
                default -> { }
            }
            assertThatThrownBy(() -> bindings(fixture.target, fixture.transport, steps, invalid.equals("version") ? 2 : 1).resolve(fixture.target))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(fixture.paths).isEmpty();
            assertThat(fixture.decrypts).hasValue(0);
            assertThat(fixture.resolutions).hasValue(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"guard", "optional", "extraction", "invalid-json", "http", "eof", "malformed", "flood"})
    void abortsWithoutPartialEvidenceOrFurtherRequests(String failure) throws Exception {
        try (var fixture = new Fixture("JSONL", 12000, 262144, 65536)) {
            var steps = steps("JSONL");
            ExecutionError error = switch (failure) {
                case "guard" -> {
                    steps.get(1).put("guard", Map.of("name", "chatId", "operator", "equals", "value", "other"));
                    yield ExecutionError.TARGET_CONFIGURATION_INVALID;
                }
                case "optional", "extraction" -> {
                    steps.get(1).put("captures", List.of(capture("sessionId", "$.absent", failure.equals("extraction"), false)));
                    yield failure.equals("extraction") ? ExecutionError.RESPONSE_PATH_NOT_FOUND : ExecutionError.TARGET_CONFIGURATION_INVALID;
                }
                case "invalid-json" -> { fixture.intermediate = "{]"; yield ExecutionError.RESPONSE_INVALID_JSON; }
                case "http" -> { fixture.sessionStatus = 503; yield ExecutionError.HTTP_STATUS_ERROR; }
                case "eof" -> { fixture.terminal = "{\"delta\":\"partial\"}\n"; yield ExecutionError.RESPONSE_FORMAT_ERROR; }
                case "malformed" -> { fixture.terminal = "{\"delta\":\"partial\"}\n{]\n"; yield ExecutionError.RESPONSE_FORMAT_ERROR; }
                default -> {
                    fixture.terminal = "\n".repeat(1025) + "{\"delta\":\"partial\"}\n{\"done\":true}\n";
                    yield ExecutionError.RESPONSE_TOO_LARGE;
                }
            };
            var adapter = fixture.resolve(steps);
            var result = legacy(adapter.interact(fixture.target, "probe", budget(30000)));
            assertThat(result.errorReason()).isEqualTo(error);
            assertThat(result.responseText()).isNull();
            int expected = switch (failure) {
                case "guard" -> 1;
                case "eof", "malformed", "flood" -> 3;
                default -> 2;
            };
            assertThat(fixture.paths).hasSize(expected);
            assertThat(fixture.decrypts).hasValue(expected);
            if (expected < 3) assertThat(result.requestBody()).isNull();
            if (failure.equals("optional")) {
                fixture.firstOverride = "{\"privateValue\":\"" + PRIVATE + "\"}";
                assertThat(adapter.interact(fixture.target, "probe", budget(30000)).errorReason()).isEqualTo(ExecutionError.RESPONSE_PATH_NOT_FOUND);
                assertThat(fixture.paths).hasSize(expected + 1);
            }
        }
    }

    @Test void rateWaitConsumesGlobalDeadlineWithoutMaterializingNextRequest() throws Exception {
        try (var fixture = new Fixture("SSE", 1000, 262144, 65536)) {
            var result = fixture.resolve(steps("SSE")).interact(fixture.target, "probe", budget(30000));
            assertThat(result.errorReason()).isEqualTo(ExecutionError.TARGET_TIMEOUT);
            assertThat(fixture.paths).hasSize(1);
            assertThat(fixture.decrypts).hasValue(1);
        }
    }

    @Test void quotaWaitUsesGlobalDeadlineButIsExcludedFromWorkflowLatency() throws Exception {
        try (var fixture = new Fixture("JSON", 12000, 262144, 65536)) {
            var adapter = fixture.resolve(steps("JSON"));
            long started = System.nanoTime();
            var result = legacy(adapter.interact(fixture.target, "probe", budget(30000)));
            long wallMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            assertThat(result.errorReason()).isNull();
            assertThat(result.latencyMs()).isNotNegative();
            assertThat(wallMs - result.latencyMs()).isGreaterThanOrEqualTo(1800);
            assertThat(fixture.paths).hasSize(3);
        }
        try (var fixture = new Fixture("JSON", 1800, 262144, 65536)) {
            var result = legacy(fixture.resolve(steps("JSON")).interact(fixture.target, "probe", budget(30000)));
            assertThat(result.errorReason()).isEqualTo(ExecutionError.TARGET_TIMEOUT);
            assertThat(result.responseText()).isNull();
            assertThat(fixture.paths).hasSize(2);
            assertThat(fixture.decrypts).hasValue(2);
        }
    }

    @Test void capturesFromRootArrayAndReusesValueInDependentStep() throws Exception {
        try (var fixture = new Fixture("JSON", 12000, 262144, 65536)) {
            var steps = steps("JSON");
            steps.getFirst().put("captures", List.of(capture("chatId", "$[0].id", true, false),
                    capture("privateHandle", "$[0].privateValue", true, true)));
            fixture.firstOverride = "[{\"id\":\"chat-1\",\"privateValue\":\"" + PRIVATE + "\"}]";
            var result = legacy(fixture.resolve(steps).interact(fixture.target, "probe", budget(30000)));
            assertThat(result.errorReason()).isNull();
            assertThat(result.responseText()).isEqualTo("respuesta final é [REDACTED_CREDENTIAL]");
            assertThat(fixture.paths).containsExactly("/api/chats", "/api/chats/chat-1/sessions",
                    "/api/chats/chat-1/sessions/session-1/answer");
            assertThat(fixture.badRequests).hasValue(0);
        }
    }

    @Test void scanDeadlineAlsoStopsNextStep() throws Exception {
        try (var fixture = new Fixture("SSE", 12000, 262144, 65536)) {
            var result = fixture.resolve(steps("SSE")).interact(fixture.target, "probe", budget(900));
            assertThat(result.errorReason()).isEqualTo(ExecutionError.SCAN_BUDGET_EXCEEDED);
            assertThat(fixture.paths).hasSize(1);
            assertThat(fixture.decrypts).hasValue(1);
        }
    }

    @Test void cumulativeResponseBytesIncludeIgnoredStreamData() throws Exception {
        try (var fixture = new Fixture("SSE", 12000, 150, 65536)) {
            fixture.terminal = ":" + "x".repeat(100) + "\ndata: {\"delta\":\"partial\"}\n\ndata: [DONE]\n\n";
            var result = fixture.resolve(steps("SSE")).interact(fixture.target, "probe", budget(30000));
            assertThat(result.errorReason()).isEqualTo(ExecutionError.RESPONSE_TOO_LARGE);
            assertThat(result.responseText()).isNull();
            assertThat(fixture.paths).hasSize(3);
        }
    }

    @Test void cumulativeRequestBytesPreventNextExchangeBeforeDnsOrCredential() throws Exception {
        try (var fixture = new Fixture("SSE", 12000, 262144, 30)) {
            var result = fixture.resolve(steps("SSE")).interact(fixture.target, "x".repeat(12), budget(30000));
            assertThat(result.errorReason()).isEqualTo(ExecutionError.REQUEST_TOO_LARGE);
            assertThat(fixture.paths).hasSize(2);
            assertThat(fixture.resolutions).hasValue(2);
            assertThat(fixture.decrypts).hasValue(2);
        }
    }

    @Test void cancellationClosesInteractionAndNeverSendsDependentStep() throws Exception {
        try (var fixture = new Fixture("SSE", 12000, 262144, 65536)) {
            fixture.holdFirst = true;
            var adapter = fixture.resolve(steps("SSE"));
            var result = new FutureTask<>(() -> adapter.interact(fixture.target, "probe", budget(30000)));
            var worker = Thread.ofPlatform().start(result);
            try {
                assertThat(fixture.holding.await(5, TimeUnit.SECONDS)).isTrue();
                worker.interrupt();
                assertThat(result.get(4, TimeUnit.SECONDS).errorReason()).isEqualTo(ExecutionError.EXECUTION_INTERRUPTED);
                assertThat(fixture.paths).hasSize(1);
                assertThat(fixture.decrypts).hasValue(1);
            } finally {
                fixture.release.countDown();
                worker.interrupt();
            }
        }
    }

    private static InvocationResult legacy(NormalizedInteraction result) { return LegacyInvocationResultProjection.toLegacy(result); }

    private static ScanBudget budget(int ms) { return new ScanBudget(Instant.now(), ms, Clock.systemUTC(), System::nanoTime); }

    private static Map<String, Object> capture(String name, String path, boolean required, boolean sensitive) {
        return SessionTargetTest.capture(name, "JSON", path, required, sensitive, 128);
    }

    private static Map<String, Object> response(String type) {
        return switch (type) {
            case "SSE" -> Map.of("type", type, "stream", Map.of("mediaType", "text/event-stream",
                    "textPath", "$.delta", "mode", "DELTA", "terminalLiteral", "[DONE]"));
            case "JSONL" -> Map.of("type", type, "stream", Map.of("mediaType", "application/x-ndjson",
                    "textPath", "$.delta", "mode", "DELTA", "terminalLiteral", "{\"done\":true}"));
            default -> Map.of("type", "JSON", "textPath", "$.text");
        };
    }

    static List<Map<String, Object>> steps(String type) {
        return new ArrayList<>(List.of(new HashMap<>(Map.of("id", "chat", "method", "POST", "path", "/api/chats",
                        "body", Map.of(), "dependsOn", List.of(), "response", Map.of("type", "JSON"),
                        "captures", List.of(capture("chatId", "$.chatId", true, false), capture("privateHandle", "$.privateValue", true, true)))),
                new HashMap<>(Map.of("id", "session", "method", "POST", "path", "/api/chats/{{chatId}}/sessions",
                        "body", Map.of("chat", "{{chatId}}"), "dependsOn", List.of("chat"), "response", Map.of("type", "JSON"),
                        "headers", Map.of("Accept-Language", "es"), "captures", List.of(capture("sessionId", "$.sessionId", true, false)))),
                new HashMap<>(Map.of("id", "answer", "method", "POST", "path", "/api/chats/{{chatId}}/sessions/{{sessionId}}/answer",
                        "body", Map.of("prompt", "{{PROMPT}}"), "dependsOn", List.of("chat", "session"), "response", response(type)))));
    }

    private LocalWorkflowBindings bindings(TargetConfiguration target, HttpTargetInvocationAdapter transport,
            List<Map<String, Object>> steps, int version) throws Exception {
        Path file = directory.resolve("workflows.json");
        var entry = Map.of("projectId", target.projectId(), "targetConfigurationId", target.id(),
                "targetUpdatedAt", target.updatedAt().toString(), "workflowId", "fixture", "revision", 1, "steps", steps);
        Files.writeString(file, JsonMapper.shared().writeValueAsString(Map.of("schemaVersion", version, "bindings", List.of(entry))));
        return new LocalWorkflowBindings(file, transport);
    }

    private static HttpTargetInvocationAdapter transport(TargetDestinationPolicy policy, BoundedDnsResolver dns,
            AtomicInteger decrypts, ExecutionLimits limits) {
        return new HttpTargetInvocationAdapter(policy, dns,
                new TargetJsonProcessor(new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of()))),
                ignored -> {
                    decrypts.incrementAndGet();
                    return new EphemeralCredential(FIXTURE_CREDENTIAL.getBytes(StandardCharsets.UTF_8));
                }, limits);
    }

    private static TargetConfiguration target(String origin, int timeout) {
        var now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return new TargetConfiguration(UUID.randomUUID(), UUID.randomUUID(),
                new TargetSettings(null, new TargetUrl(origin + "/unused"), "{\"prompt\":\"{{PROMPT}}\"}", "$.text",
                        Map.of("Accept", "application/json"), AuthType.BEARER_TOKEN, null, timeout),
                new EncryptedCredential(new byte[] {1}), now, now);
    }

    static final class Logs implements AutoCloseable {
        private final Logger logger = (Logger) LoggerFactory.getLogger(LocalWorkflowBindings.class);
        private final Level previous = logger.getLevel();
        private final ListAppender<ILoggingEvent> events = new ListAppender<>();
        Logs() {
            events.start();
            logger.addAppender(events);
            logger.setLevel(Level.DEBUG);
        }
        String messages() { return String.join("\n", events.list.stream().map(ILoggingEvent::getFormattedMessage).toList()); }
        @Override public void close() { logger.detachAppender(events); logger.setLevel(previous); events.stop(); }
    }

    private final class Fixture implements AutoCloseable {
        final HttpServer server;
        final BoundedDnsResolver dns;
        final HttpTargetInvocationAdapter transport;
        final TargetConfiguration target;
        final AtomicInteger decrypts = new AtomicInteger(), resolutions = new AtomicInteger(), badRequests = new AtomicInteger();
        final List<String> paths = new CopyOnWriteArrayList<>();
        final CountDownLatch holding = new CountDownLatch(1), release = new CountDownLatch(1);
        volatile String intermediate, terminal, firstOverride;
        volatile int sessionStatus = 200;
        volatile boolean holdFirst;
        int chat;

        Fixture(String type, int timeout, int responseBytes, int requestBytes) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            String origin = "http://workflow-fixture.invalid:" + server.getAddress().getPort();
            String terminalMedia = type.equals("SSE") ? "text/event-stream" : type.equals("JSONL") ? "application/x-ndjson" : "application/json";
            terminal = switch (type) {
                case "SSE" -> ": ignored\n\ndata: {\"delta\":\"respuesta \"}\n\ndata: {\"delta\":\"final é " + PRIVATE + "\"}\n\ndata: [DONE]\n\n";
                case "JSONL" -> "{\"delta\":\"respuesta \"}\n{\"delta\":\"final é " + PRIVATE + "\"}\n{\"done\":true}\n";
                default -> "{\"text\":\"respuesta final é " + PRIVATE + "\"}";
            };
            server.createContext("/", exchange -> {
                try (exchange) {
                    String path = exchange.getRequestURI().getRawPath();
                    paths.add(path);
                    String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    boolean first = path.equals("/api/chats");
                    boolean last = path.endsWith("/answer");
                    String media = last ? terminalMedia : "application/json";
                    if (!media.equals(exchange.getRequestHeaders().getFirst("Accept"))
                            || !("Bearer " + FIXTURE_CREDENTIAL).equals(exchange.getRequestHeaders().getFirst("Authorization"))
                            || exchange.getRequestHeaders().containsKey("Cookie")
                            || !first && !last && (!request.equals("{\"chat\":\"chat-" + chat + "\"}")
                                || !"es".equals(exchange.getRequestHeaders().getFirst("Accept-Language")))) {
                        badRequests.incrementAndGet();
                        exchange.sendResponseHeaders(400, -1);
                        return;
                    }
                    String response;
                    if (first) {
                        chat++;
                        exchange.getResponseHeaders().set("Set-Cookie", "fixture=ignored; Path=/");
                        response = firstOverride == null ? "{\"chatId\":\"chat-" + chat + "\",\"privateValue\":\"" + PRIVATE + "\"}" : firstOverride;
                    } else if (last) response = terminal;
                    else response = intermediate == null ? "{\"sessionId\":\"session-" + chat + "\"}" : intermediate;
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", media);
                    exchange.sendResponseHeaders(last ? 201 : first ? 200 : sessionStatus, last || holdFirst ? 0 : bytes.length);
                    if (first && holdFirst) {
                        exchange.getResponseBody().write(bytes[0]);
                        exchange.getResponseBody().flush();
                        holding.countDown();
                        try { release.await(8, TimeUnit.SECONDS); }
                        catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                        return;
                    }
                    for (int i = 0; i < bytes.length; i += 3) {
                        exchange.getResponseBody().write(bytes, i, Math.min(3, bytes.length - i));
                        if (last) exchange.getResponseBody().flush();
                    }
                }
            });
            server.start();
            dns = new BoundedDnsResolver(host -> {
                resolutions.incrementAndGet();
                return new InetAddress[] {InetAddress.getLoopbackAddress()};
            });
            var limits = new ExecutionLimits(4, requestBytes, responseBytes, 65536, 131072, 524288, 300000, 2000, 5000, 1000);
            transport = transport(new TargetDestinationPolicy(Set.of(), true, true, Set.of(origin)), dns, decrypts, limits);
            target = target(origin, timeout);
        }

        TargetAdapter resolve(List<Map<String, Object>> steps) throws Exception {
            return bindings(target, transport, steps, 1).resolve(target);
        }
        @Override public void close() { release.countDown(); server.stop(0); transport.close(); dns.close(); }
    }
}
