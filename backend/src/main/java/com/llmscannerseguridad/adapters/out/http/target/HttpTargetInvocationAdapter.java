package com.llmscannerseguridad.adapters.out.http.target;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.adapters.out.validation.target.TargetJsonProcessor;
import com.llmscannerseguridad.application.execution.InvocationResult;
import com.llmscannerseguridad.application.execution.ScanBudget;
import com.llmscannerseguridad.application.execution.TargetInvocationPort;
import com.llmscannerseguridad.application.target.CredentialDecryptionPort;
import com.llmscannerseguridad.application.target.EphemeralCredential;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.AuthType;
import com.llmscannerseguridad.domain.target.TargetConfiguration;
import com.llmscannerseguridad.domain.target.TargetRules;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.ManagedHttpClientConnectionFactory;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.Timeout;

public final class HttpTargetInvocationAdapter implements TargetInvocationPort, AutoCloseable {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpTargetInvocationAdapter.class);
    private final TargetDestinationPolicy destinations;
    private final BoundedDnsResolver dns;
    private final TargetJsonProcessor json;
    private final CredentialDecryptionPort crypto;
    private final ExecutionLimits limits;
    private final InvocationGate gate;
    private final SSLConnectionSocketFactory tls;
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1, task -> {
        var thread = new Thread(task, "target-deadline");
        thread.setDaemon(true);
        return thread;
    });

    public HttpTargetInvocationAdapter(TargetDestinationPolicy destinations, BoundedDnsResolver dns,
            TargetJsonProcessor json, CredentialDecryptionPort crypto, ExecutionLimits limits) {
        this(destinations, dns, json, crypto, limits, SSLConnectionSocketFactory.getSocketFactory());
    }

    HttpTargetInvocationAdapter(TargetDestinationPolicy destinations, BoundedDnsResolver dns,
            TargetJsonProcessor json, CredentialDecryptionPort crypto, ExecutionLimits limits, SSLConnectionSocketFactory tls) {
        this.destinations = destinations;
        this.dns = dns;
        this.json = json;
        this.crypto = crypto;
        this.limits = limits;
        this.tls = tls;
        gate = new InvocationGate(limits.intervalMs());
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    @Override
    public InvocationResult invoke(TargetConfiguration target, String prompt, ScanBudget budget) {
        return invokeWithResponseExtractor(target, prompt, budget,
                value -> json.render(target.settings(), value), body -> json.extract(body, target.settings().responsePath()));
    }

    public InvocationResult invokeWithResponseExtractor(TargetConfiguration target, String prompt, ScanBudget budget,
            java.util.function.Function<String, String> requestRenderer, TargetResponseTextExtractor extractor) {
        return exchange(target, prompt, budget, requestRenderer, extractor, null);
    }

    InvocationResult invokeStream(TargetConfiguration target, String prompt, ScanBudget budget, StreamBinding binding) {
        if (!binding.permits(target.settings())) return InvocationResult.failure(ExecutionError.TARGET_CONFIGURATION_INVALID);
        return exchange(target, prompt, budget, value -> json.render(target.settings(), value), null, binding);
    }

    private InvocationResult exchange(TargetConfiguration target, String prompt, ScanBudget budget,
            java.util.function.Function<String, String> requestRenderer, TargetResponseTextExtractor extractor, StreamBinding binding) {
        long entered = System.nanoTime();
        try {
            if (binding == null) gate.acquire(budget);
            else gate.acquire(budget, entered + TimeUnit.MILLISECONDS.toNanos(target.settings().timeoutMs()));
        } catch (InvocationFailure error) {
            return InvocationResult.failure(error.reason);
        }
        long started = System.nanoTime();
        long allowed = Math.min(TimeUnit.MILLISECONDS.toNanos(target.settings().timeoutMs()), budget.remainingNanos());
        long targetDeadline = (binding == null ? started : entered) + TimeUnit.MILLISECONDS.toNanos(target.settings().timeoutMs());
        boolean scanLimited = started + allowed < targetDeadline;
        long deadline = Math.min(started + allowed, targetDeadline);
        var collector = binding == null ? null : new StreamCollector(binding, json, limits, () -> checkStream(deadline, budget));
        ExecutionError streamError = null;
        Integer httpStatus = null;
        String safeRequest = null;
        boolean requestRedacted = false;
        byte[] body = null;
        try {
            if (binding == null) check(deadline, budget); else checkStream(deadline, budget);
            var settings = target.settings();
            boolean demo = destinations.authorize(settings.url());
            URI uri = URI.create(settings.url().value());
            if (settings.authType() != AuthType.NONE && uri.getScheme().equalsIgnoreCase("http") && !demo) {
                throw new InvocationFailure(ExecutionError.TARGET_TRANSPORT_REJECTED);
            }
            String rendered;
            try {
                rendered = requestRenderer.apply(prompt);
            } catch (RuntimeException error) {
                throw new InvocationFailure(ExecutionError.TARGET_CONFIGURATION_INVALID);
            }
            body = rendered.getBytes(StandardCharsets.UTF_8);
            if (body.length > limits.requestBytes()) {
                throw new InvocationFailure(ExecutionError.REQUEST_TOO_LARGE);
            }
            String host = uri.getHost().replace("[", "").replace("]", "");
            var address = destinations.select(dns.resolve(host,
                    Math.min(TimeUnit.MILLISECONDS.toNanos(limits.dnsMs()), deadline - System.nanoTime())), demo);
            if (binding == null) check(deadline, budget); else checkStream(deadline, budget);
            var resolver = new DnsResolver() {
                @Override public InetAddress[] resolve(String ignored) { return new InetAddress[] {address}; }
                @Override public String resolveCanonicalHostname(String original) { return original; }
            };
            var http1 = Http1Config.custom().setMaxHeaderCount(100).setMaxLineLength(8192).build();
            var factory = new ManagedHttpClientConnectionFactory(http1, null, new LimitedResponseParserFactory());
            var sockets = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", tls).build();
            var manager = new BasicHttpClientConnectionManager(sockets, factory,
                    DefaultSchemePortResolver.INSTANCE, resolver);
            long remainingMs = Math.max(1, (deadline - System.nanoTime() + 999999) / 1000000);
            manager.setConnectionConfig(ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(Math.min(limits.connectMs(), remainingMs)))
                    .setSocketTimeout(Timeout.ofMilliseconds(Math.min(limits.connectMs(), remainingMs))).build());
            manager.setTlsConfig(TlsConfig.custom()
                    .setHandshakeTimeout(Timeout.ofMilliseconds(Math.min(limits.connectMs(), remainingMs))).build());
            var request = new HttpPost(uri);
            request.setVersion(HttpVersion.HTTP_1_1);
            request.setConfig(RequestConfig.custom().setRedirectsEnabled(false).setAuthenticationEnabled(false)
                    .setContentCompressionEnabled(false).setProtocolUpgradeEnabled(false)
                    .setResponseTimeout(Timeout.ofMilliseconds(remainingMs))
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(remainingMs)).build());
            request.setEntity(new ByteArrayEntity(body, ContentType.APPLICATION_JSON));
            settings.headers().forEach(request::setHeader);
            if (binding != null) {
                request.removeHeaders("Accept");
                request.setHeader("Accept", binding.mediaType());
            } else if (!request.containsHeader("Accept")) {
                request.setHeader("Accept", "application/json");
            }
            request.setHeader("Accept-Encoding", "identity");
            request.setHeader("Connection", "close");
            try (manager; var client = HttpClients.custom().setConnectionManager(manager)
                    .setRoutePlanner(new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE))
                    .disableAutomaticRetries().disableRedirectHandling().disableCookieManagement()
                    .disableContentCompression().disableAuthCaching().disableConnectionState()
                    .setConnectionReuseStrategy((req, response, context) -> false).build()) {
                EphemeralCredential credential = null;
                try {
                    String secret = null;
                    if (settings.authType() != AuthType.NONE) {
                        try {
                            credential = crypto.decrypt(target.credential());
                            secret = credential.value();
                            TargetRules.secret(secret, settings.authType());
                            if (!StandardCharsets.ISO_8859_1.newEncoder().canEncode(secret)) {
                                throw new InvocationFailure(ExecutionError.CREDENTIAL_UNAVAILABLE);
                            }
                        } catch (RuntimeException error) {
                            throw new InvocationFailure(ExecutionError.CREDENTIAL_UNAVAILABLE);
                        }
                        request.setHeader(settings.authType() == AuthType.BEARER_TOKEN ? "Authorization" : settings.authHeaderName(),
                                settings.authType() == AuthType.BEARER_TOKEN ? "Bearer " + secret : secret);
                    }
                    var evidence = json.redact(rendered, null, secret);
                    safeRequest = evidence.request();
                    requestRedacted = evidence.requestRedacted();
                    evidenceLimit(safeRequest, null);
                    check(deadline, budget);
                    var cancellation = scheduler.schedule(() -> {
                        request.cancel();
                        manager.close(CloseMode.IMMEDIATE);
                    }, Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                    Thread caller = Thread.currentThread();
                    var interruption = binding == null ? null : scheduler.scheduleWithFixedDelay(() -> {
                        if (caller.isInterrupted()) {
                            request.cancel();
                            manager.close(CloseMode.IMMEDIATE);
                        }
                    }, 25, 25, TimeUnit.MILLISECONDS);
                    try {
                        var response = client.execute(request);
                        httpStatus = response.getCode();
                        if (httpStatus < 100 || httpStatus > 599) {
                            httpStatus = null;
                            throw new InvocationFailure(ExecutionError.RESPONSE_FORMAT_ERROR);
                        }
                        if (httpStatus < 200 || httpStatus > 299) {
                            throw new InvocationFailure(ExecutionError.HTTP_STATUS_ERROR);
                        }
                        String contentType = response.getFirstHeader("Content-Type") == null ? ""
                                : response.getFirstHeader("Content-Type").getValue();
                        if (response.getHeaders("Content-Type").length != 1 || !(binding == null
                                ? jsonContentType(contentType) : streamingContentType(contentType, binding.mediaType()))
                                || Arrays.stream(response.getHeaders("Content-Encoding"))
                                    .anyMatch(header -> !header.getValue().equalsIgnoreCase("identity"))) {
                            throw new InvocationFailure(ExecutionError.RESPONSE_FORMAT_ERROR);
                        }
                        var entity = response.getEntity();
                        if (entity == null) {
                            throw new InvocationFailure(binding == null ? ExecutionError.RESPONSE_INVALID_JSON : ExecutionError.RESPONSE_FORMAT_ERROR);
                        }
                        if (entity.getContentLength() > limits.responseBytes()) {
                            throw new InvocationFailure(ExecutionError.RESPONSE_TOO_LARGE);
                        }
                        // No cerrar el stream mediante drain: se cierra la conexión en finally.
                        String text = binding == null ? extractor.extract(readResponse(entity.getContent(), deadline, budget))
                                : collector.collect(entity.getContent());
                        if (text.getBytes(StandardCharsets.UTF_8).length > limits.textBytes()) {
                            throw new InvocationFailure(ExecutionError.EVIDENCE_LIMIT_EXCEEDED);
                        }
                        evidence = json.redact(rendered, text, secret);
                        if (binding != null && TargetRules.bytes(evidence.response()) > limits.textBytes()) {
                            throw new InvocationFailure(ExecutionError.EVIDENCE_LIMIT_EXCEEDED);
                        }
                        evidenceLimit(evidence.request(), evidence.response());
                        if (binding == null) check(deadline, budget); else checkStream(deadline, budget);
                        return new InvocationResult(httpStatus, elapsed(started), null, evidence.request(), evidence.response(),
                                evidence.requestRedacted(), evidence.responseRedacted());
                    } finally {
                        if (interruption != null) interruption.cancel(false);
                        cancellation.cancel(false);
                        request.cancel();
                        manager.close(CloseMode.IMMEDIATE);
                    }
                } finally {
                    if (credential != null) {
                        credential.close();
                    }
                    request.removeHeaders("Authorization");
                    if (settings.authHeaderName() != null) {
                        request.removeHeaders(settings.authHeaderName());
                    }
                }
            }
        } catch (TargetJsonProcessor.JsonFailure error) {
            streamError = error.reason();
            return failure(error.reason(), httpStatus, started, deadline, scanLimited, budget, safeRequest, requestRedacted);
        } catch (InvocationFailure error) {
            streamError = binding != null && Thread.currentThread().isInterrupted() ? ExecutionError.EXECUTION_INTERRUPTED
                    : binding != null && error.reason == ExecutionError.DNS_RESOLUTION_FAILED && System.nanoTime() >= deadline
                            ? ExecutionError.TARGET_TIMEOUT : error.reason;
            return failure(streamError, httpStatus, started, deadline, scanLimited, budget, safeRequest, requestRedacted);
        } catch (SocketTimeoutException error) {
            streamError = ExecutionError.TARGET_TIMEOUT;
            return failure(ExecutionError.TARGET_TIMEOUT, httpStatus, started, deadline, scanLimited, budget, safeRequest, requestRedacted);
        } catch (LimitedResponseParserFactory.HeaderLimitException error) {
            streamError = ExecutionError.RESPONSE_FORMAT_ERROR;
            return failure(ExecutionError.RESPONSE_FORMAT_ERROR, httpStatus, started, deadline, scanLimited, budget, safeRequest, requestRedacted);
        } catch (org.apache.hc.core5.http.MessageConstraintException | org.apache.hc.client5.http.ClientProtocolException error) {
            streamError = ExecutionError.RESPONSE_FORMAT_ERROR;
            return failure(ExecutionError.RESPONSE_FORMAT_ERROR, httpStatus, started, deadline, scanLimited, budget, safeRequest, requestRedacted);
        } catch (IOException error) {
            streamError = binding != null && Thread.currentThread().isInterrupted() ? ExecutionError.EXECUTION_INTERRUPTED
                    : binding != null && httpStatus != null && System.nanoTime() < deadline ? ExecutionError.RESPONSE_FORMAT_ERROR
                    : ExecutionError.TARGET_CONNECTION_FAILED;
            return failure(streamError, httpStatus, started, deadline, scanLimited, budget, safeRequest, requestRedacted);
        } catch (RuntimeException error) {
            streamError = ExecutionError.EXECUTION_INTERNAL_ERROR;
            return failure(ExecutionError.EXECUTION_INTERNAL_ERROR, httpStatus, started, deadline, scanLimited, budget, safeRequest, requestRedacted);
        } finally {
            if (body != null) {
                Arrays.fill(body, (byte) 0);
            }
            gate.release();
            if (collector != null) {
                LOG.debug("stream events={} bytes={} durationMs={} terminal={} error={}", collector.units(), collector.bytes(),
                        elapsed(entered), collector.terminal(), effectiveError(streamError, deadline, scanLimited, budget));
            }
        }
    }

    private InvocationResult failure(ExecutionError error, Integer http, long started, long deadline, boolean scanLimited,
            ScanBudget budget, String request, boolean redacted) {
        error = effectiveError(error, deadline, scanLimited, budget);
        if (error == ExecutionError.EVIDENCE_LIMIT_EXCEEDED) {
            request = null;
            redacted = false;
        }
        return new InvocationResult(http, elapsed(started), error, request, null, redacted, false);
    }

    private static ExecutionError effectiveError(ExecutionError error, long deadline, boolean scanLimited, ScanBudget budget) {
        if (error == null) return null;
        if (budget.remainingNanos() == 0 || (scanLimited && System.nanoTime() >= deadline)) {
            error = ExecutionError.SCAN_BUDGET_EXCEEDED;
        } else if (System.nanoTime() >= deadline && error == ExecutionError.TARGET_CONNECTION_FAILED) {
            error = ExecutionError.TARGET_TIMEOUT;
        }
        return error;
    }

    private static long elapsed(long started) {
        return Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
    }

    private String readResponse(InputStream input, long deadline, ScanBudget budget) throws IOException {
        byte[] received = new byte[limits.responseBytes() + 1];
        java.nio.CharBuffer decoded = null;
        try {
            int length = 0;
            for (int count; (count = input.read(received, length, Math.min(8192, received.length - length))) != -1;) {
                check(deadline, budget);
                length += count;
                if (length > limits.responseBytes()) {
                    throw new InvocationFailure(ExecutionError.RESPONSE_TOO_LARGE);
                }
            }
            try {
                decoded = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(received, 0, length));
                return decoded.toString();
            } catch (java.nio.charset.CharacterCodingException error) {
                throw new InvocationFailure(ExecutionError.RESPONSE_INVALID_JSON);
            }
        } finally {
            Arrays.fill(received, (byte) 0);
            if (decoded != null && decoded.hasArray()) {
                Arrays.fill(decoded.array(), '\0');
            }
        }
    }

    private static void check(long deadline, ScanBudget budget) {
        if (budget.remainingNanos() == 0) {
            throw new InvocationFailure(ExecutionError.SCAN_BUDGET_EXCEEDED);
        }
        if (System.nanoTime() >= deadline) {
            throw new InvocationFailure(ExecutionError.TARGET_TIMEOUT);
        }
    }

    private static void checkStream(long deadline, ScanBudget budget) {
        if (Thread.currentThread().isInterrupted()) throw new InvocationFailure(ExecutionError.EXECUTION_INTERRUPTED);
        check(deadline, budget);
    }

    private static boolean streamingContentType(String raw, String expected) {
        try {
            var type = ContentType.parse(raw);
            return expected.equalsIgnoreCase(type.getMimeType())
                    && (type.getCharset() == null || type.getCharset().equals(StandardCharsets.UTF_8));
        } catch (RuntimeException error) { return false; }
    }

    private static boolean jsonContentType(String raw) {
        try {
            var type = ContentType.parse(raw);
            String media = type.getMimeType().toLowerCase(Locale.ROOT);
            return (media.equals("application/json") || (media.startsWith("application/") && media.endsWith("+json")))
                    && (type.getCharset() == null || type.getCharset().equals(StandardCharsets.UTF_8));
        } catch (RuntimeException error) {
            return false;
        }
    }

    private void evidenceLimit(String request, String response) {
        int requestBytes = request == null ? 0 : request.getBytes(StandardCharsets.UTF_8).length;
        if (requestBytes > limits.requestBytes() || requestBytes + json.serializedTextBytes(response) > limits.evidenceBytes()) {
            throw new InvocationFailure(ExecutionError.EVIDENCE_LIMIT_EXCEEDED);
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
