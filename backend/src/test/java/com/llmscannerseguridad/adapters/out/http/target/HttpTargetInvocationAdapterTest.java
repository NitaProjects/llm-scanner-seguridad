package com.llmscannerseguridad.adapters.out.http.target;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.llmscannerseguridad.adapters.out.crypto.target.AesGcmCredentialEncryptionAdapter;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.adapters.out.validation.target.*;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.application.target.CredentialDecryptionPort;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class HttpTargetInvocationAdapterTest {
    private final AesGcmCredentialEncryptionAdapter crypto = new AesGcmCredentialEncryptionAdapter(
            () -> Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void postJsonHeadersBothAuthModesAndExactRedaction() throws Exception {
        try (var server = new ControlledTargetServer();
                var dns = new BoundedDnsResolver(host -> { throw new AssertionError("No DNS para literal."); });
                var adapter = adapter(server, dns, crypto, ExecutionLimits.defaults())) {
            for (AuthType type : AuthType.values()) {
                var result = adapter.invoke(target(server.origin() + "/echo", type, 10000, "{\"m\":\"{{PROMPT}}\",\"known\":\"fixture-secret\"}"),
                        "comillas \" 😀\n", budget());
                assertThat(result.errorReason()).isNull();
                assertThat(result.httpStatus()).isEqualTo(200);
                var received = server.received.getLast();
                assertThat(received.method()).isEqualTo("POST");
                assertThat(received.ordinary()).isEqualTo("ordinary-value");
                assertThat(new StrictTargetJson().read(received.body())).isInstanceOf(Map.class);
                if (type == AuthType.NONE) {
                    assertThat(received.authorization()).isNull();
                    assertThat(received.apiKey()).isNull();
                } else {
                    assertThat(result.requestBody()).doesNotContain("fixture-secret");
                    assertThat(result.responseText()).doesNotContain("fixture-secret").contains("[REDACTED_CREDENTIAL]");
                    assertThat(result.requestRedacted()).isTrue();
                    assertThat(result.responseRedacted()).isTrue();
                    assertThat(type == AuthType.BEARER_TOKEN ? received.authorization() : received.apiKey())
                            .isEqualTo(type == AuthType.BEARER_TOKEN ? "Bearer fixture-secret" : "fixture-secret");
                }
            }
            for (int i = 1; i < server.received.size(); i++) {
                assertThat(server.received.get(i).receivedNanos() - server.received.get(i - 1).receivedNanos())
                        .isGreaterThanOrEqualTo(1000000000L);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {201, 401, 403, 429, 500, 301, 302, 303, 307, 308})
    void neverRetriesOrFollowsRedirects(int status) throws Exception {
        try (var server = new ControlledTargetServer(); var dns = new BoundedDnsResolver(InetAddress::getAllByName);
                var adapter = adapter(server, dns, crypto, ExecutionLimits.defaults())) {
            var result = adapter.invoke(target(server.origin() + "/status/" + status, AuthType.NONE, 10000, "{\"m\":\"{{PROMPT}}\"}"),
                    "prompt", budget());
            assertThat(result.httpStatus()).isEqualTo(status);
            assertThat(result.errorReason()).isEqualTo(status < 300 ? null : ExecutionError.HTTP_STATUS_ERROR);
            assertThat(server.received).hasSize(1);
            if (status >= 300) assertThat(result.responseText()).isNull();
        }
    }

    @Test
    void technicalFormatPathSizeTimeoutAndUnicodeFailures() throws Exception {
        try (var server = new ControlledTargetServer(); var dns = new BoundedDnsResolver(InetAddress::getAllByName);
                var adapter = adapter(server, dns, crypto, ExecutionLimits.defaults())) {
            var cases = Map.of("/invalid", ExecutionError.RESPONSE_INVALID_JSON, "/missing", ExecutionError.RESPONSE_PATH_NOT_FOUND,
                    "/nontext", ExecutionError.RESPONSE_NOT_TEXT, "/format", ExecutionError.RESPONSE_FORMAT_ERROR,
                    "/large", ExecutionError.RESPONSE_TOO_LARGE, "/slow", ExecutionError.TARGET_TIMEOUT);
            for (var entry : cases.entrySet()) {
                var result = adapter.invoke(target(server.origin() + entry.getKey(), AuthType.NONE, 1000, "{\"m\":\"{{PROMPT}}\"}"),
                        "prompt", budget());
                assertThat(result.errorReason()).as(entry.getKey()).isEqualTo(entry.getValue());
            }
            assertThat(adapter.invoke(target(server.origin() + "/dangerous", AuthType.NONE, 10000, "{\"m\":\"{{PROMPT}}\"}"),
                    "prompt", budget()).errorReason()).isNull();
            assertThat(adapter.invoke(target(server.origin() + "/nul", AuthType.NONE, 10000, "{\"m\":\"{{PROMPT}}\"}"),
                    "prompt", budget()).responseText()).isEqualTo("a\0😀");
            int count = server.received.size();
            assertThat(adapter.invoke(target(server.origin(), AuthType.NONE, 10000, "{\"m\":\"{{PROMPT}}\"}"),
                    "x".repeat(65536), budget()).errorReason()).isEqualTo(ExecutionError.REQUEST_TOO_LARGE);
            assertThat(server.received).hasSize(count);
        }
    }

    @Test
    void authorizationPrecedesDnsAndCryptoAndBadSharedCredentialNeverSends(CapturedOutput output) throws Exception {
        var calls = new AtomicInteger();
        var forbiddenCrypto = mock(CredentialDecryptionPort.class);
        try (var dns = new BoundedDnsResolver(host -> { calls.incrementAndGet(); throw new java.net.UnknownHostException("PRIVATE_DNS_MARKER"); });
                var adapter = new HttpTargetInvocationAdapter(new TargetDestinationPolicy(Set.of(), false, false, Set.of(), Set.of()),
                        dns, json(), forbiddenCrypto, ExecutionLimits.defaults())) {
            var result = adapter.invoke(target("https://controlled.invalid", AuthType.BEARER_TOKEN, 10000, "{\"m\":\"{{PROMPT}}\"}"), "prompt", budget());
            assertThat(result.errorReason()).isEqualTo(ExecutionError.TARGET_NOT_AUTHORIZED);
            assertThat(calls).hasValue(0);
            verifyNoInteractions(forbiddenCrypto);
            assertThat(result.toString()).doesNotContain("PRIVATE_DNS_MARKER", "fixture-secret");
        }
        try (var server = new ControlledTargetServer(); var dns = new BoundedDnsResolver(InetAddress::getAllByName);
                var adapter = adapter(server, dns, encrypted -> { throw new IllegalStateException("PRIVATE_CRYPTO_MARKER"); }, ExecutionLimits.defaults())) {
            var result = adapter.invoke(target(server.origin(), AuthType.BEARER_TOKEN, 10000, "{\"m\":\"{{PROMPT}}\"}"), "prompt", budget());
            assertThat(result.errorReason()).isEqualTo(ExecutionError.CREDENTIAL_UNAVAILABLE);
            assertThat(server.received).isEmpty();
        }
        assertThat(output.getAll()).doesNotContain("PRIVATE_CRYPTO_MARKER", "PRIVATE_DNS_MARKER", "fixture-secret");
    }

    @Test
    void singleValidatedDnsAnswerPinsConnectionAndRebindingIsRejectedNextTime() throws Exception {
        try (var server = new ControlledTargetServer()) {
            String origin = server.origin().replace("127.0.0.1", "fixture.invalid");
            var calls = new AtomicInteger();
            try (var dns = new BoundedDnsResolver(host -> new InetAddress[] {InetAddress.getByName(
                        calls.incrementAndGet() == 1 ? "127.0.0.1" : "169.254.169.254")});
                    var adapter = new HttpTargetInvocationAdapter(new TargetDestinationPolicy(Set.of(), true, true, Set.of(origin), Set.of()),
                            dns, json(), crypto, ExecutionLimits.defaults())) {
                assertThat(adapter.invoke(target(origin, AuthType.NONE, 10000, "{\"m\":\"{{PROMPT}}\"}"), "prompt", budget()).errorReason()).isNull();
                assertThat(calls).hasValue(1);
                assertThat(server.received).hasSize(1);
                assertThat(adapter.invoke(target(origin, AuthType.NONE, 10000, "{\"m\":\"{{PROMPT}}\"}"), "prompt", budget()).errorReason())
                        .isEqualTo(ExecutionError.TARGET_ADDRESS_BLOCKED);
                assertThat(server.received).hasSize(1);
                assertThat(calls).hasValue(2);
            }
        }
    }

    private HttpTargetInvocationAdapter adapter(ControlledTargetServer server, BoundedDnsResolver dns,
            CredentialDecryptionPort decryption, ExecutionLimits limits) {
        return new HttpTargetInvocationAdapter(new TargetDestinationPolicy(Set.of(), true, true, Set.of(server.origin()), Set.of()),
                dns, json(), decryption, limits);
    }

    @Test
    void tlsRejectsUntrustedCertificateAndWrongOriginalHostnameEvenWhenCertificateIsTrusted() throws Exception {
        try (var server = new ControlledHttpsServer(); var dns = new BoundedDnsResolver(InetAddress::getAllByName)) {
            var destination = new TargetDestinationPolicy(Set.of(), true, true, Set.of(server.origin()), Set.of());
            try (var ordinary = new HttpTargetInvocationAdapter(destination, dns, json(), crypto, ExecutionLimits.defaults());
                    var trustedCertificateOnly = new HttpTargetInvocationAdapter(destination, dns, json(), crypto,
                            ExecutionLimits.defaults(), org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder.create()
                                .setSslContext(server.trustedContext).build())) {
                for (var adapter : java.util.List.of(ordinary, trustedCertificateOnly)) {
                    var result = adapter.invoke(target(server.origin(), AuthType.NONE, 10000, "{\"m\":\"{{PROMPT}}\"}"), "prompt", budget());
                    assertThat(result.errorReason()).isEqualTo(ExecutionError.TARGET_CONNECTION_FAILED);
                    assertThat(server.received).hasValue(0);
                }
            }
        }
    }

    @Test
    void trustedDnsCertificateUsesPinnedAddressAndOriginalHostAndSni() throws Exception {
        String hostname = "tls.fixture.invalid";
        var resolutions = new AtomicInteger();
        try (var server = new ControlledHttpsServer(hostname);
                var dns = new BoundedDnsResolver(host -> {
                    assertThat(host).isEqualTo(hostname);
                    resolutions.incrementAndGet();
                    return new InetAddress[] {InetAddress.getByName("127.0.0.1")};
                })) {
            String origin = server.origin().replace("127.0.0.1", hostname);
            var destination = new TargetDestinationPolicy(Set.of(), true, true, Set.of(origin), Set.of());
            try (var adapter = new HttpTargetInvocationAdapter(destination, dns, json(), crypto, ExecutionLimits.defaults(),
                    org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder.create().setSslContext(server.trustedContext).build())) {
                var result = adapter.invoke(target(origin, AuthType.NONE, 10000, "{\"m\":\"{{PROMPT}}\"}"), "prompt", budget());
                assertThat(result.errorReason()).isNull();
                assertThat(result.httpStatus()).isEqualTo(200);
                assertThat(result.responseText()).isEqualTo("ok");
                assertThat(resolutions).hasValue(1);
                assertThat(server.received).hasValue(1);
                assertThat(server.hostHeader).hasValue(origin.substring("https://".length()));
                assertThat(server.sniHostname).hasValue(hostname);
            }
        }
    }

    @Test
    void exactAndExceededRequestResponseTextAndSerializedEvidenceLimits() throws Exception {
        try (var server = new ControlledTargetServer(); var dns = new BoundedDnsResolver(InetAddress::getAllByName)) {
            var target = target(server.origin() + "/text/8", AuthType.NONE, 1000, "{\"m\":\"{{PROMPT}}\"}");
            var exact = new ExecutionLimits(4, 16, 19, 8, 26, 100, 60000, 2000, 5000, 1000);
            try (var adapter = adapter(server, dns, crypto, exact)) {
                assertThat(adapter.invoke(target, "12345678", budget()).errorReason()).isNull();
                assertThat(adapter.invoke(target, "123456789", budget()).errorReason()).isEqualTo(ExecutionError.REQUEST_TOO_LARGE);
                assertThat(server.received).hasSize(1);
            }
            for (var changed : java.util.List.of(
                    new ExecutionLimits(4, 16, 18, 8, 26, 100, 60000, 2000, 5000, 1000),
                    new ExecutionLimits(4, 16, 19, 7, 26, 100, 60000, 2000, 5000, 1000),
                    new ExecutionLimits(4, 16, 19, 8, 25, 100, 60000, 2000, 5000, 1000))) {
                try (var adapter = adapter(server, dns, crypto, changed)) {
                    assertThat(adapter.invoke(target, "12345678", budget()).errorReason()).isEqualTo(
                            changed.responseBytes() == 18 ? ExecutionError.RESPONSE_TOO_LARGE : ExecutionError.EVIDENCE_LIMIT_EXCEEDED);
                }
            }
        }
    }

    @Test
    void slowBodyCannotResetDeadlineAndInvalidEncodingHeadersAndJsonAreClosedErrors() throws Exception {
        try (var server = new ControlledTargetServer(); var dns = new BoundedDnsResolver(InetAddress::getAllByName);
                var adapter = adapter(server, dns, crypto, ExecutionLimits.defaults())) {
            for (var path : java.util.List.of("/utf8", "/duplicate", "/deep", "/compressed", "/manyheaders", "/slowbody")) {
                var result = adapter.invoke(target(server.origin() + path, AuthType.NONE, 1000, "{\"m\":\"{{PROMPT}}\"}"), "prompt", budget());
                var expected = path.equals("/slowbody") ? ExecutionError.TARGET_TIMEOUT
                        : path.equals("/compressed") || path.equals("/manyheaders") ? ExecutionError.RESPONSE_FORMAT_ERROR
                        : ExecutionError.RESPONSE_INVALID_JSON;
                assertThat(result.errorReason()).as(path).isEqualTo(expected);
                assertThat(result.responseText()).isNull();
            }
        }
    }

    @Test
    void globalBudgetPrecedesIndividualTimeoutAndPublicHttpAuthNeverResolvesOrDecrypts() throws Exception {
        var forbiddenCrypto = mock(CredentialDecryptionPort.class);
        try (var dns = new BoundedDnsResolver(host -> { throw new AssertionError("DNS no autorizado."); });
                var adapter = new HttpTargetInvocationAdapter(new TargetDestinationPolicy(Set.of("http://controlled.invalid"), false, false, Set.of(), Set.of()),
                        dns, json(), forbiddenCrypto, ExecutionLimits.defaults())) {
            var result = adapter.invoke(target("http://controlled.invalid", AuthType.BEARER_TOKEN, 1000, "{\"m\":\"{{PROMPT}}\"}"), "prompt", budget());
            assertThat(result.errorReason()).isEqualTo(ExecutionError.TARGET_TRANSPORT_REJECTED);
            verifyNoInteractions(forbiddenCrypto);
        }
        try (var server = new ControlledTargetServer(); var dns = new BoundedDnsResolver(InetAddress::getAllByName);
                var adapter = adapter(server, dns, crypto, ExecutionLimits.defaults())) {
            var shortBudget = new ScanBudget(Instant.now(), 100, Clock.systemUTC(), System::nanoTime);
            var result = adapter.invoke(target(server.origin() + "/slow", AuthType.NONE, 1000, "{\"m\":\"{{PROMPT}}\"}"), "prompt", shortBudget);
            assertThat(result.errorReason()).isEqualTo(ExecutionError.SCAN_BUDGET_EXCEEDED);
        }
    }

    @Test
    void stalledTlsHandshakeIsCancelledWithoutAnyHttpRequest() throws Exception {
        var release = new java.util.concurrent.CountDownLatch(1);
        try (var listener = new java.net.ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                var fixture = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var accepted = fixture.submit(() -> {
                try (var socket = listener.accept()) {
                    release.await(5, java.util.concurrent.TimeUnit.SECONDS);
                }
                return null;
            });
            String origin = "https://127.0.0.1:" + listener.getLocalPort();
            var limits = new ExecutionLimits(4, 65536, 262144, 65536, 131072, 524288, 300000, 2000, 100, 1000);
            try (var dns = new BoundedDnsResolver(InetAddress::getAllByName);
                    var adapter = new HttpTargetInvocationAdapter(new TargetDestinationPolicy(Set.of(), true, true, Set.of(origin), Set.of()),
                            dns, json(), crypto, limits)) {
                var result = adapter.invoke(target(origin, AuthType.NONE, 1000, "{\"m\":\"{{PROMPT}}\"}"), "prompt", budget());
                assertThat(result.errorReason()).isEqualTo(ExecutionError.TARGET_TIMEOUT);
                assertThat(result.httpStatus()).isNull();
                assertThat(result.latencyMs()).isLessThan(900L);
            } finally {
                release.countDown();
            }
            accepted.get(3, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    private TargetJsonProcessor json() {
        return new TargetJsonProcessor(new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of())));
    }

    private TargetConfiguration target(String url, AuthType auth, int timeout, String template) {
        var now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        return new TargetConfiguration(UUID.randomUUID(), UUID.randomUUID(), new TargetSettings(null, new TargetUrl(url), template,
                "$.text", Map.of("X-Demo", "ordinary-value"), auth, auth == AuthType.API_KEY_HEADER ? "X-API-Key" : null, timeout),
                auth == AuthType.NONE ? null : crypto.encrypt("fixture-secret"), now, now);
    }

    private ScanBudget budget() {
        return new ScanBudget(Instant.now(), 300000, Clock.systemUTC(), System::nanoTime);
    }
}
