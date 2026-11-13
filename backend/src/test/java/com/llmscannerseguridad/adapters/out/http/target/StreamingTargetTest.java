package com.llmscannerseguridad.adapters.out.http.target;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.adapters.out.validation.target.*;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.application.target.EphemeralCredential;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class StreamingTargetTest {
    private static final String SSE = "text/event-stream";
    private static final String DELTA = "event: content\ndata: {\"text\":\"partial\"}\n\n";
    private static final String DONE = "event: end\n\n";
    @TempDir Path directory;

    @Test void negotiatesSseAndReconstructsMultilineDataWithSplitUtf8AndRedaction() throws Exception {
        String stream = "\uFEFF: heartbeat\r\n\r\nevent: ignored\rdata: ignored\r\revent: content\r\n"
                + "data: {\r\ndata: \"text\":\"Hola 😀 synthetic-\"}\r\n\r\n"
                + "event: content\ndata: {\"text\":\"bearer\"}\n\n" + DONE;
        try (var fixture = new Fixture(SSE, stream, true, ExecutionLimits.defaults())) {
            fixture.holdOpen = true;
            var target = fixture.target(AuthType.BEARER_TOKEN, null, 3000);
            var result = fixture.resolve(target, SSE, Map.of()).interact(target, "prompt", budget());
            assertThat(result.responseText()).isEqualTo("Hola 😀 [REDACTED_CREDENTIAL]");
            assertThat(result.responseRedacted()).isTrue();
            assertThat(result.errorReason()).isNull();
            var legacy = LegacyInvocationResultProjection.toLegacy(result);
            assertThat(legacy.httpStatus()).isEqualTo(200);
            assertThat(legacy.requestBody()).isEqualTo("{\"m\":\"prompt\"}");
            assertThat(fixture.requests).hasValue(1);
            assertThat(fixture.decryptions).hasValue(1);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"application/x-ndjson", "application/jsonl"})
    void negotiatesBothJsonlTypesAndReplacesSnapshots(String media) throws Exception {
        try (var fixture = new Fixture(media, "{\"type\":\"content\",\"text\":\"old\"}\r\n"
                + "{\"type\":\"ignored\"}\n{\"type\":\"content\",\"text\":\"último 😀\"}\n{\"type\":\"end\"}\n", true, ExecutionLimits.defaults())) {
            var target = fixture.target(AuthType.NONE, null, 3000);
            var result = fixture.resolve(target, media, Map.of("mode", "SNAPSHOT", "eventPath", "$.type")).interact(target, "prompt", budget());
            assertThat(result.errorReason()).isNull();
            assertThat(result.responseText()).isEqualTo("último 😀");
            assertThat(fixture.requests).hasValue(1);
        }
    }

    @Test void rejectsAcceptCollisionsBeforeDnsCredentialsOrRequests() throws Exception {
        try (var fixture = new Fixture(SSE, DELTA + DONE, false, ExecutionLimits.defaults())) {
            var credential = fixture.target(fixture.origin.replace("127.0.0.1", "fixture.invalid"), AuthType.API_KEY_HEADER, "aCcEpT", 3000);
            assertThatThrownBy(() -> fixture.resolve(credential, SSE, Map.of())).hasMessage("Binding streaming no válido.");
            var ordinary = fixture.target(fixture.origin.replace("127.0.0.1", "fixture.invalid"), AuthType.BEARER_TOKEN, null, 3000);
            assertThatThrownBy(() -> fixture.resolve(ordinary, SSE, Map.of("reservedHeaders", List.of("aCcEpT"))))
                    .hasMessage("Binding streaming no válido.");
            assertThat(fixture.requests).hasValue(0);
            assertThat(fixture.decryptions).hasValue(0);
            assertThat(fixture.lookups).hasValue(0);
        }
    }

    @Test void snapshotsExactBindingAndRejectsStaleOrMalformedManifest() throws Exception {
        try (var fixture = new Fixture(SSE, DELTA + DONE, false, ExecutionLimits.defaults())) {
            var target = fixture.target(AuthType.NONE, null, 3000);
            assertThat(new LocalStreamingBindings(null, fixture.transport).resolve(target)).isNull();
            var fixed = fixture.resolve(target, SSE, Map.of());
            assertThatThrownBy(() -> fixture.resolve(target, SSE, Map.of("targetUpdatedAt", Instant.EPOCH.toString())))
                    .hasMessage("Binding streaming no válido.");
            assertThatThrownBy(() -> fixture.resolve(target, SSE, Map.of("unknown", true))).hasMessage("Binding streaming no válido.");
            assertThat(fixture.requests).hasValue(0);
            assertThat(fixed.interact(target, "prompt", budget()).responseText()).isEqualTo("partial");
        }
    }

    @Test void prematureEofTerminalWithoutContentAndRemoteErrorDiscardPartial() throws Exception {
        failure(SSE, DELTA, ExecutionLimits.defaults(), ExecutionError.RESPONSE_FORMAT_ERROR);
        failure(SSE, DONE, ExecutionLimits.defaults(), ExecutionError.RESPONSE_FORMAT_ERROR);
        failure(SSE, DELTA + "event: error\ndata: private\n\n" + DONE, ExecutionLimits.defaults(), ExecutionError.RESPONSE_FORMAT_ERROR);
    }

    @Test void literalTerminalsUseSseDataAndCompleteJsonlObjectWithoutDelimiter() throws Exception {
        var validator = new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of()));
        var json = new TargetJsonProcessor(validator);
        for (var media : List.of(SSE, "application/jsonl")) {
            String literal = media.equals(SSE) ? "[DONE]" : "{\"done\":true}";
            String body = media.equals(SSE) ? DELTA + "data: " + literal + "\n\n"
                    : "{\"text\":\"partial\"}\r\n" + literal + "\r\n";
            var binding = new StreamBinding(media, null, List.of("text"), null, null, null, literal,
                    StreamBinding.Mode.DELTA, Set.of());
            var collector = new StreamCollector(binding, json, ExecutionLimits.defaults(), () -> { });
            assertThat(collector.collect(new java.io.ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))))
                    .isEqualTo("partial");
            assertThat(collector.terminal()).isTrue();
        }
    }

    @Test void malformedJsonlAndWrongContentTypeDiscardPartial() throws Exception {
        failure("application/jsonl", "{\"type\":\"content\",\"text\":\"partial\"}\n{broken}\n{\"type\":\"end\"}\n",
                ExecutionLimits.defaults(), ExecutionError.RESPONSE_FORMAT_ERROR);
        try (var fixture = new Fixture(SSE, DELTA + DONE, false, ExecutionLimits.defaults(), "application/json")) {
            var target = fixture.target(AuthType.NONE, null, 3000);
            assertFailure(fixture.resolve(target, SSE, Map.of()).interact(target, "prompt", budget()), ExecutionError.RESPONSE_FORMAT_ERROR);
        }
    }

    @Test void jsonlAllowsOnlyEmptyLinesAndParsesObjectsWithJsonWhitespace() throws Exception {
        String body = " \t{\"type\":\"content\",\"text\":\"complete\"} \t\n\n\r\n{\"type\":\"end\"}\r\n";
        try (var fixture = new Fixture("application/jsonl", body, false, ExecutionLimits.defaults())) {
            var target = fixture.target(AuthType.NONE, null, 3000);
            var result = fixture.resolve(target, "application/jsonl", Map.of("eventPath", "$.type"))
                    .interact(target, "prompt", budget());
            assertThat(result.errorReason()).isNull();
            assertThat(result.responseText()).isEqualTo("complete");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"\u2003", "\u3000", " \t"})
    void jsonlNonEmptyWhitespaceLineFailsBetweenDeltaAndTerminal(String malformed) throws Exception {
        failure("application/jsonl", "{\"type\":\"content\",\"text\":\"partial\"}\n" + malformed + "\n{\"type\":\"end\"}\n",
                ExecutionLimits.defaults(), ExecutionError.RESPONSE_FORMAT_ERROR);
    }

    @Test void sseAllowsExactly1024LinesIncludingIgnoredFieldsAndTerminal() throws Exception {
        // Cinco líneas para DELTA y DONE, incluidas sus líneas vacías.
        String body = "id: x\nretry: 1\nunused: x\n".repeat(339) + "id: x\nid: x\n" + DELTA + DONE;
        try (var fixture = new Fixture(SSE, body, false, ExecutionLimits.defaults())) {
            var target = fixture.target(AuthType.NONE, null, 3000);
            var result = fixture.resolve(target, SSE, Map.of()).interact(target, "prompt", budget());
            assertThat(result.errorReason()).isNull();
            assertThat(result.responseText()).isEqualTo("partial");
        }
    }

    @Test void sseRejectsIgnoredLinesOverQuotaBeforeAndAfterContent() throws Exception {
        failure(SSE, "id: x\n".repeat(1025) + DELTA + DONE, ExecutionLimits.defaults(), ExecutionError.RESPONSE_TOO_LARGE);
        failure(SSE, DELTA + "retry: 1\n".repeat(1020) + DONE, ExecutionLimits.defaults(), ExecutionError.RESPONSE_TOO_LARGE);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void closesConnectionOnInvalidJsonlOrIgnoredSseFloodWithoutWaitingForEof(boolean sse) throws Exception {
        String media = sse ? SSE : "application/jsonl";
        String body = sse ? DELTA + "id: x\n".repeat(1025)
                : "{\"type\":\"content\",\"text\":\"partial\"}\n\u2003\n";
        var validator = new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of()));
        try (var server = new java.net.ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                var worker = java.util.concurrent.Executors.newSingleThreadExecutor();
                var dns = new BoundedDnsResolver(InetAddress::getAllByName)) {
            server.setSoTimeout(4000);
            String origin = "http://127.0.0.1:" + server.getLocalPort();
            var closed = worker.submit(() -> {
                try (var socket = server.accept()) {
                    socket.setSoTimeout(4000);
                    var input = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    int length = 0;
                    for (String line; (line = input.readLine()) != null && !line.isEmpty();) {
                        if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) length = Integer.parseInt(line.substring(15).strip());
                    }
                    assertThat(input.skip(length)).isEqualTo(length);
                    byte[] frame = body.getBytes(StandardCharsets.UTF_8);
                    var output = socket.getOutputStream();
                    output.write(("HTTP/1.1 200 OK\r\nContent-Type: " + media + "\r\nTransfer-Encoding: chunked\r\n\r\n"
                            + Integer.toHexString(frame.length) + "\r\n").getBytes(StandardCharsets.US_ASCII));
                    output.write(frame);
                    output.write("\r\n".getBytes(StandardCharsets.US_ASCII));
                    output.flush();
                    // El servidor mantiene el stream abierto hasta observar el cierre del cliente.
                    try {
                        return input.read() == -1;
                    } catch (java.net.SocketException reset) {
                        // El cierre inmediato puede verse como FIN o RST según el sistema.
                        assertThat(reset).hasMessageContaining("reset");
                        return true;
                    }
                }
            });
            try (var transport = new HttpTargetInvocationAdapter(new TargetDestinationPolicy(Set.of(), true, true, Set.of(origin)),
                    dns, new TargetJsonProcessor(validator), encrypted -> { throw new AssertionError("Sin credencial"); }, ExecutionLimits.defaults())) {
                Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
                var target = new TargetConfiguration(UUID.randomUUID(), UUID.randomUUID(), new TargetSettings(null, new TargetUrl(origin),
                        "{\"m\":\"{{PROMPT}}\"}", "$.text", Map.of(), AuthType.NONE, null, 3000), null, now, now);
                var adapter = new StreamBinding(media, sse ? null : List.of("type"), List.of("text"), "content", "error", "end", null,
                        StreamBinding.Mode.DELTA, Set.of());
                var result = LegacyInvocationResultProjection.fromLegacy(transport.invokeStream(target, "prompt", budget(), adapter));
                assertFailure(result, sse ? ExecutionError.RESPONSE_TOO_LARGE : ExecutionError.RESPONSE_FORMAT_ERROR);
                assertThat(closed.get(4, TimeUnit.SECONDS)).isTrue();
            }
        }
    }

    @Test void rejectsHeartbeatFloodFrameLimitAndEntityLimit() throws Exception {
        failure(SSE, DELTA + ":h\n\n".repeat(1024) + DONE, ExecutionLimits.defaults(), ExecutionError.RESPONSE_TOO_LARGE);
        failure(SSE, DELTA + ":" + "x".repeat(32768) + "\n\n" + DONE, ExecutionLimits.defaults(), ExecutionError.RESPONSE_TOO_LARGE);
        var small = new ExecutionLimits(4, 65536, 100, 65536, 131072, 524288, 300000, 2000, 5000, 1000);
        failure(SSE, DELTA + ":" + "x".repeat(80) + "\n\n" + DONE, small, ExecutionError.RESPONSE_TOO_LARGE);
    }

    @Test void timeoutAndInterruptionDiscardPartialWithoutWaitingForEof() throws Exception {
        for (boolean interrupt : List.of(false, true)) {
            try (var fixture = new Fixture(SSE, DELTA, false, ExecutionLimits.defaults())) {
                fixture.holdOpen = true;
                var target = fixture.target(AuthType.NONE, null, interrupt ? 10000 : 1000);
                var adapter = fixture.resolve(target, SSE, Map.of());
                var result = new java.util.concurrent.CompletableFuture<NormalizedInteraction>();
                var worker = new Thread(() -> result.complete(adapter.interact(target, "prompt", budget())));
                worker.start();
                try {
                    assertThat(fixture.flushed.await(2, TimeUnit.SECONDS)).isTrue();
                    if (interrupt) worker.interrupt();
                    assertFailure(result.get(3, TimeUnit.SECONDS), interrupt ? ExecutionError.EXECUTION_INTERRUPTED : ExecutionError.TARGET_TIMEOUT);
                } finally {
                    fixture.release.countDown();
                    worker.join(3000);
                }
            }
        }
    }

    private void failure(String media, String body, ExecutionLimits limits, ExecutionError error) throws Exception {
        try (var fixture = new Fixture(media, body, false, limits)) {
            var target = fixture.target(AuthType.NONE, null, 3000);
            assertFailure(fixture.resolve(target, media, media.equals(SSE) ? Map.of() : Map.of("eventPath", "$.type"))
                    .interact(target, "prompt", budget()), error);
            assertThat(fixture.requests).hasValue(1);
        }
    }

    private static void assertFailure(NormalizedInteraction result, ExecutionError error) {
        assertThat(result.errorReason()).isEqualTo(error);
        assertThat(result.responseText()).isNull();
    }

    private static ScanBudget budget() { return new ScanBudget(Instant.now(), 300000, Clock.systemUTC(), System::nanoTime); }

    private final class Fixture implements AutoCloseable {
        final HttpServer server;
        final HttpTargetInvocationAdapter transport;
        final BoundedDnsResolver dns;
        final String origin;
        final AtomicInteger requests = new AtomicInteger(), decryptions = new AtomicInteger(), lookups = new AtomicInteger();
        final CountDownLatch flushed = new CountDownLatch(1), release = new CountDownLatch(1);
        volatile boolean holdOpen;

        Fixture(String accept, String stream, boolean split, ExecutionLimits limits) throws Exception {
            this(accept, stream, split, limits, accept);
        }

        Fixture(String accept, String stream, boolean split, ExecutionLimits limits, String contentType) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            origin = "http://127.0.0.1:" + server.getAddress().getPort();
            server.createContext("/", exchange -> {
                try (exchange) {
                    requests.incrementAndGet();
                    exchange.getRequestBody().readAllBytes();
                    if (!List.of(accept).equals(exchange.getRequestHeaders().get("Accept"))) {
                        exchange.sendResponseHeaders(406, -1);
                        return;
                    }
                    exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
                    exchange.sendResponseHeaders(200, 0);
                    byte[] bytes = stream.getBytes(StandardCharsets.UTF_8);
                    if (split) for (byte value : bytes) {
                        exchange.getResponseBody().write(value);
                        exchange.getResponseBody().flush();
                    } else exchange.getResponseBody().write(bytes);
                    exchange.getResponseBody().flush();
                    flushed.countDown();
                    if (holdOpen) release.await(12, TimeUnit.SECONDS);
                } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            });
            server.start();
            dns = new BoundedDnsResolver(host -> { lookups.incrementAndGet(); return new InetAddress[] {InetAddress.getLoopbackAddress()}; });
            var policy = new LocalTargetUrlPolicy(false, false, Set.of());
            transport = new HttpTargetInvocationAdapter(new TargetDestinationPolicy(Set.of(), true, true,
                    Set.of(origin, origin.replace("127.0.0.1", "fixture.invalid"))), dns,
                    new TargetJsonProcessor(new JsonTargetConfigurationValidator(policy)), encrypted -> {
                        decryptions.incrementAndGet();
                        return new EphemeralCredential("synthetic-bearer".getBytes(StandardCharsets.UTF_8));
                    }, limits);
        }

        TargetConfiguration target(AuthType auth, String header, int timeout) {
            return target(origin, auth, header, timeout);
        }

        TargetConfiguration target(String url, AuthType auth, String header, int timeout) {
            Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            return new TargetConfiguration(UUID.randomUUID(), UUID.randomUUID(), new TargetSettings(null, new TargetUrl(url),
                    "{\"m\":\"{{PROMPT}}\"}", "$.text", header == null ? Map.of("Accept", "application/json") : Map.of(), auth, header, timeout),
                    auth == AuthType.NONE ? null : new EncryptedCredential(new byte[] {1}), now, now);
        }

        TargetAdapter resolve(TargetConfiguration target, String media, Map<String, ?> options) throws Exception {
            var binding = new HashMap<String, Object>(Map.of("projectId", target.projectId().toString(), "targetConfigurationId", target.id().toString(),
                    "targetUpdatedAt", target.updatedAt().toString(), "mediaType", media, "textPath", "$.text", "mode", "DELTA",
                    "contentEvent", "content", "errorEvent", "error", "terminalEvent", "end"));
            binding.putAll(options);
            var file = directory.resolve("stream.json");
            Files.writeString(file, tools.jackson.databind.json.JsonMapper.builder().build()
                    .writeValueAsString(Map.of("schemaVersion", 1, "bindings", List.of(binding))));
            return new LocalStreamingBindings(file, transport).resolve(target);
        }

        @Override public void close() { release.countDown(); server.stop(0); transport.close(); dns.close(); }
    }
}
