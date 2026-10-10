package com.llmscannerseguridad.adapters.out.evaluation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import com.llmscannerseguridad.adapters.out.http.target.BoundedDnsResolver;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.domain.evaluation.LlmError;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.Timeout;

public final class OpenAiEvaluatorAdapter implements EvaluatorPort, AutoCloseable {
    record HttpReply(int status, String body) { }
    @FunctionalInterface interface Transport {
        HttpReply exchange(String body, EvaluationCallBudget budget) throws Exception;
    }
    // El destino no procede del target ni de la configuración REST.
    private static final String ENDPOINT = "https://api.openai.com/v1/responses";
    private final EvaluatorSettings settings;
    private final EvaluatorMessages messages;
    private final Supplier<String> key;
    private final EvaluatorInvocationGate gate;
    private final Transport fixtureTransport;
    private final BoundedDnsResolver dns = new BoundedDnsResolver(InetAddress::getAllByName);
    private final ScheduledThreadPoolExecutor deadlines = new ScheduledThreadPoolExecutor(1, task -> {
        var thread = new Thread(task, "evaluator-deadline"); thread.setDaemon(true); return thread;
    });
    public OpenAiEvaluatorAdapter(EvaluatorSettings settings, EvaluatorMessages messages, Supplier<String> key) {
        this(settings, messages, key, null, new EvaluatorInvocationGate(settings.intervalMs()));
    }
    OpenAiEvaluatorAdapter(EvaluatorSettings settings, EvaluatorMessages messages, Supplier<String> key,
            Transport fixtureTransport, EvaluatorInvocationGate gate) {
        this.settings = settings; this.messages = messages; this.key = key;
        this.gate = gate;
        this.fixtureTransport = fixtureTransport;
        deadlines.setRemoveOnCancelPolicy(true);
        deadlines.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }
    @Override public EvaluatorOutcome evaluate(EvaluatorInput input, EvaluationCallBudget budget) {
        String apiKey = key.get();
        if (apiKey == null || apiKey.isBlank() || apiKey.codePoints().anyMatch(c -> c < 33 || c > 126)) {
            return EvaluatorOutcome.failure(LlmError.EVALUATOR_CONFIGURATION_INVALID);
        }
        String body = messages.request(input, settings);
        if (EvaluationJson.bytes(body) > settings.inputBytes() || EvaluationJson.bytes(input.evidenceText()) > settings.evidenceBytes()) {
            return EvaluatorOutcome.failure(LlmError.EVALUATOR_INVALID_OUTPUT);
        }
        boolean acquired = false;
        long started = System.nanoTime();
        try {
            acquired = gate.acquire(budget);
            if (!acquired) return EvaluatorOutcome.failure(LlmError.EVALUATOR_TIMEOUT);
            if (fixtureTransport != null) {
                var reply = fixtureTransport.exchange(body, budget);
                if (budget.remainingNanos() <= 0) return EvaluatorOutcome.failure(LlmError.EVALUATOR_TIMEOUT);
                if (reply.status() < 200 || reply.status() >= 300) return EvaluatorOutcome.failure(classify(reply.status()));
                if (EvaluationJson.bytes(reply.body()) > settings.responseBytes()) return EvaluatorOutcome.failure(LlmError.EVALUATOR_INVALID_OUTPUT);
                return parseResponse(reply.body(), input, Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)));
            }
            var addresses = dns.resolve("api.openai.com", Math.min(2000000000L, budget.remainingNanos()));
            if (addresses.length == 0 || addresses.length > 16) return EvaluatorOutcome.failure(LlmError.EVALUATOR_UNAVAILABLE);
            var resolver = new DnsResolver() {
                @Override public InetAddress[] resolve(String host) { return new InetAddress[] {addresses[0]}; }
                @Override public String resolveCanonicalHostname(String host) { return host; }
            };
            try (var manager = new BasicHttpClientConnectionManager(null, null, DefaultSchemePortResolver.INSTANCE, resolver)) {
                long remainingMs = Math.max(1, TimeUnit.NANOSECONDS.toMillis(budget.remainingNanos()));
                manager.setConnectionConfig(ConnectionConfig.custom().setConnectTimeout(Timeout.ofMilliseconds(Math.min(5000, remainingMs)))
                        .setSocketTimeout(Timeout.ofMilliseconds(remainingMs)).build());
                manager.setTlsConfig(TlsConfig.custom().setHandshakeTimeout(Timeout.ofMilliseconds(Math.min(5000, remainingMs))).build());
                var request = new HttpPost(ENDPOINT);
                request.setConfig(RequestConfig.custom().setRedirectsEnabled(false).setAuthenticationEnabled(false)
                        .setContentCompressionEnabled(false).setProtocolUpgradeEnabled(false)
                        .setResponseTimeout(Timeout.ofMilliseconds(remainingMs)).build());
                request.setHeader("Authorization", "Bearer " + apiKey);
                request.setHeader("Accept", "application/json");
                request.setHeader("Accept-Encoding", "identity");
                request.setHeader("Connection", "close");
                request.setEntity(new ByteArrayEntity(body.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON));
                try (var client = HttpClients.custom().setConnectionManager(manager)
                        .setRoutePlanner(new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE))
                        .disableAutomaticRetries().disableRedirectHandling().disableCookieManagement()
                        .disableContentCompression().disableAuthCaching().disableConnectionState()
                        .setConnectionReuseStrategy((req, response, context) -> false).build()) {
                    if (budget.remainingNanos() <= 0) return EvaluatorOutcome.failure(LlmError.EVALUATOR_TIMEOUT);
                    var cancel = deadlines.schedule(() -> { request.cancel(); manager.close(CloseMode.IMMEDIATE); },
                            budget.remainingNanos(), TimeUnit.NANOSECONDS);
                    try {
                        var response = client.execute(request);
                        if (response.getCode() < 200 || response.getCode() >= 300) return EvaluatorOutcome.failure(classify(response.getCode()));
                        var contentType = response.getFirstHeader("Content-Type");
                        if (contentType == null || !contentType.getValue().toLowerCase(Locale.ROOT).startsWith("application/json")
                                || response.getHeaders("Content-Type").length != 1
                                || Arrays.stream(response.getHeaders("Content-Encoding")).anyMatch(h -> !h.getValue().equalsIgnoreCase("identity"))) {
                            return EvaluatorOutcome.failure(LlmError.EVALUATOR_INVALID_OUTPUT);
                        }
                        var entity = response.getEntity();
                        if (entity == null || entity.getContentLength() > settings.responseBytes()) return EvaluatorOutcome.failure(LlmError.EVALUATOR_INVALID_OUTPUT);
                        String received = EvaluationJson.read(entity.getContent(), settings.responseBytes());
                        if (budget.remainingNanos() <= 0) return EvaluatorOutcome.failure(LlmError.EVALUATOR_TIMEOUT);
                        return parseResponse(received, input, Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)));
                    } finally {
                        cancel.cancel(false); request.cancel(); request.removeHeaders("Authorization"); manager.close(CloseMode.IMMEDIATE);
                    }
                }
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt(); return EvaluatorOutcome.failure(LlmError.EVALUATOR_TIMEOUT);
        } catch (SocketTimeoutException error) {
            return EvaluatorOutcome.failure(LlmError.EVALUATOR_TIMEOUT);
        } catch (IllegalArgumentException error) {
            return EvaluatorOutcome.failure(LlmError.EVALUATOR_INVALID_OUTPUT);
        } catch (Exception error) {
            return EvaluatorOutcome.failure(budget.remainingNanos() <= 0 ? LlmError.EVALUATOR_TIMEOUT : LlmError.EVALUATOR_UNAVAILABLE);
        } finally { if (acquired) gate.release(); }
    }
    public static LlmError classify(int status) {
        if (status == 429) return LlmError.EVALUATOR_RATE_LIMITED;
        if (status >= 500 && status <= 599) return LlmError.EVALUATOR_UNAVAILABLE;
        return LlmError.EVALUATOR_CONFIGURATION_INVALID;
    }
    @SuppressWarnings("unchecked")
    public EvaluatorOutcome parseResponse(String response, EvaluatorInput input, long latencyMs) {
        try {
            var parsed = EvaluationJson.parse(response);
            if (!(parsed instanceof Map<?,?> root) || !"completed".equals(root.get("status"))
                    || root.get("error") != null || root.get("incomplete_details") != null
                    || !(root.get("output") instanceof List<?> output)) throw EvaluationJson.invalid();
            String text = null;
            for (Object item : output) {
                if (!(item instanceof Map<?,?> message)) throw EvaluationJson.invalid();
                if ("reasoning".equals(message.get("type"))) continue; // Nunca se conserva razonamiento interno.
                if (!"message".equals(message.get("type")) || !"assistant".equals(message.get("role"))
                        || !"completed".equals(message.get("status")) || !(message.get("content") instanceof List<?> contents)) throw EvaluationJson.invalid();
                for (Object part : contents) {
                    if (!(part instanceof Map<?,?> content) || !"output_text".equals(content.get("type"))
                            || !(content.get("text") instanceof String value) || text != null) throw EvaluationJson.invalid();
                    text = value;
                }
            }
            if (text == null) throw EvaluationJson.invalid();
            var assessment = new StructuredAssessmentParser().parse(text,input);
            String resolved = safeId(root.get("model"));
            String requestId = safeId(root.get("id"));
            Long in = null, out = null;
            if (root.get("usage") instanceof Map<?,?> usage) { in = token(usage.get("input_tokens")); out = token(usage.get("output_tokens")); }
            BigDecimal cost = in != null && out != null && settings.tariffValid() && settings.model().equals(resolved)
                    ? settings.inputPricePerMillion().multiply(BigDecimal.valueOf(in)).add(settings.outputPricePerMillion().multiply(BigDecimal.valueOf(out)))
                        .divide(new BigDecimal("1000000"),6,RoundingMode.HALF_UP) : null;
            return new EvaluatorOutcome(assessment,null,resolved,latencyMs,in,out,cost,requestId);
        } catch (RuntimeException error) { return EvaluatorOutcome.failure(LlmError.EVALUATOR_INVALID_OUTPUT); }
    }
    private String safeId(Object value) { return value instanceof String text && text.matches("[A-Za-z0-9._:-]{1,128}") ? text : null; }
    private Long token(Object value) {
        try { return value instanceof BigInteger integer && integer.signum() >= 0 ? integer.longValueExact() : null; }
        catch (ArithmeticException error) { return null; }
    }
    @Override public void close() { deadlines.shutdownNow(); dns.close(); }
}
