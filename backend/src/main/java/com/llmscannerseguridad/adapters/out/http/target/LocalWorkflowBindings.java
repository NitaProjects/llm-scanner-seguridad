package com.llmscannerseguridad.adapters.out.http.target;

import static com.llmscannerseguridad.adapters.out.http.target.LocalSessionBindings.*;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.llmscannerseguridad.adapters.out.validation.target.JsonTargetConfigurationValidator;
import com.llmscannerseguridad.adapters.out.validation.target.LocalTargetUrlPolicy;
import com.llmscannerseguridad.adapters.out.validation.target.StrictTargetJson;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.*;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/** Definiciones de operador, vinculadas a una revisión exacta del target. */
public final class LocalWorkflowBindings implements TargetIntegrationPort {
    private static final int MAX_STEPS = 6;
    private static final int FILE_BYTES = 262144;
    private static final StrictTargetJson JSON = new StrictTargetJson();
    private static final JsonTargetConfigurationValidator VALIDATOR =
            new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of()));
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LocalWorkflowBindings.class);
    private final Path file;
    private final HttpTargetInvocationAdapter transport;

    private record Workflow(List<Step> steps) { }
    private record Step(String id, String method, String path, String body, Map<String, String> headers,
            List<String> dependsOn, Guard guard, List<InteractionSession.Capture> captures,
            String responsePath, StreamBinding stream) { }
    private record Guard(String name, String operator, Object expected) {
        boolean allows(InteractionSession session, TargetUrl base) {
            if (session.value(name) == null) return false;
            Object value = session.identifier(name, base);
            if (operator.equals("exists")) return true;
            return value instanceof java.math.BigInteger ? value.toString().equals(expected.toString()) : value.equals(expected);
        }
    }

    public LocalWorkflowBindings(Path file, HttpTargetInvocationAdapter transport) {
        if (file != null && (file.toString().startsWith("\\\\") || file.getFileName() == null
                || !file.getFileName().toString().endsWith(".json"))) throw invalid();
        this.file = file;
        this.transport = transport;
    }

    @Override public TargetAdapter resolve(TargetConfiguration target) {
        if (file == null) return null;
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(FILE_BYTES + 1);
            if (bytes.length > FILE_BYTES) throw invalid();
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            var root = object(JSON.read(text), Set.of("schemaVersion", "bindings"), Set.of());
            if (!Integer.valueOf(1).equals(root.get("schemaVersion")) || !(root.get("bindings") instanceof List<?> entries)
                    || entries.size() > 64) throw invalid();
            var targets = new HashSet<UUID>();
            Workflow selected = null;
            for (Object item : entries) {
                var entry = object(item, Set.of("projectId", "targetConfigurationId", "targetUpdatedAt",
                        "workflowId", "revision", "steps"), Set.of());
                UUID id = uuid(entry, "targetConfigurationId");
                UUID project = uuid(entry, "projectId");
                Instant updated = Instant.parse(string(entry, "targetUpdatedAt", 64));
                identifier(entry, "workflowId");
                if (!(entry.get("revision") instanceof Integer revision) || revision < 1
                        || !targets.add(id) || updated.getNano() % 1000 != 0) throw invalid();
                Workflow workflow = workflow(entry);
                if (!target.id().equals(id)) continue;
                var settings = target.settings();
                if (!target.projectId().equals(project) || !target.updatedAt().equals(updated)
                        || settings.adapterType() != TargetAdapterType.REST_LEGACY
                        || !Set.of(AuthType.NONE, AuthType.BEARER_TOKEN).contains(settings.authType())) throw invalid();
                for (Step step : workflow.steps()) {
                    if (step.stream() != null && !step.stream().permits(settings)) throw invalid();
                    var merged = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
                    merged.putAll(settings.headers());
                    merged.putAll(step.headers());
                    TargetRules.headers(merged, settings.authHeaderName());
                }
                selected = workflow;
            }
            if (selected == null) return null;
            Workflow fixed = selected;
            return (ignored, prompt, budget) -> execute(target, prompt, budget, fixed);
        } catch (Exception error) {
            LOG.warn("workflowBinding=TARGET_CONFIGURATION_INVALID");
            throw invalid();
        }
    }

    private static Workflow workflow(Map<?, ?> entry) {
        if (!(entry.get("steps") instanceof List<?> raw) || raw.isEmpty() || raw.size() > MAX_STEPS) throw invalid();
        var previous = new LinkedHashMap<String, Step>();
        var captureNames = new HashSet<String>();
        for (int i = 0; i < raw.size(); i++) {
            var fields = object(raw.get(i), Set.of("id", "method", "path", "dependsOn", "response"),
                    Set.of("body", "captures", "guard", "headers"));
            String id = identifier(fields, "id");
            try {
                boolean terminal = i == raw.size() - 1;
                var step = step(id, fields, previous, terminal);
                if (previous.putIfAbsent(id, step) != null) throw invalid();
                for (var capture : step.captures()) {
                    if (!captureNames.add(capture.name().toLowerCase(Locale.ROOT))) throw invalid();
                }
            } catch (RuntimeException error) {
                LOG.warn("workflowStep={} ordinal={} error=TARGET_CONFIGURATION_INVALID", id, i + 1);
                throw invalid();
            }
        }
        return new Workflow(List.copyOf(previous.values()));
    }

    private static Step step(String id, Map<?, ?> fields, Map<String, Step> previous, boolean terminal) {
        String method = string(fields, "method", 4);
        if (!Set.of("GET", "POST").contains(method) || method.equals("GET") == fields.containsKey("body")
                || terminal && (!method.equals("POST") || fields.containsKey("guard") || fields.containsKey("captures"))) throw invalid();
        if (!(fields.get("dependsOn") instanceof List<?> rawDependencies) || rawDependencies.size() > previous.size()) throw invalid();
        var dependsOn = new LinkedHashSet<String>();
        for (Object value : rawDependencies) {
            if (!(value instanceof String dependency) || !previous.containsKey(dependency) || !dependsOn.add(dependency)) throw invalid();
        }
        var available = dependsOn.stream().flatMap(dep -> previous.get(dep).captures().stream()).toList();
        String path = string(fields, "path", 2048);
        validatePath(path, available);
        String body = null;
        if (method.equals("POST")) {
            if (!(fields.get("body") instanceof Map<?, ?> template)) throw invalid();
            validateSlots(template, available);
            if (promptCount(template) != (terminal ? 1 : 0)) throw invalid();
            body = JsonMapper.shared().writeValueAsString(template);
            if (TargetRules.bytes(body) > 32768) throw invalid();
            if (terminal) VALIDATOR.template(body);
        }
        var captures = parseCaptures(fields.containsKey("captures") ? fields.get("captures") : List.of());
        if (captures.stream().anyMatch(capture -> capture.source() != InteractionSession.Source.JSON)) throw invalid();
        Guard guard = fields.containsKey("guard") ? guard(fields.get("guard"), available) : null;
        var response = object(fields.get("response"), Set.of("type"), Set.of("textPath", "stream"));
        String type = string(response, "type", 5);
        String responsePath = null;
        StreamBinding stream = null;
        if (type.equals("JSON")) {
            if (response.containsKey("stream") || terminal != response.containsKey("textPath")) throw invalid();
            if (terminal) {
                responsePath = string(response, "textPath", 1024);
                VALIDATOR.responsePath(responsePath);
            }
        } else {
            if (!terminal || !Set.of("SSE", "JSONL").contains(type) || response.containsKey("textPath")) throw invalid();
            stream = LocalStreamingBindings.binding(object(response.get("stream"), Set.of("mediaType", "textPath", "mode"),
                    Set.of("eventPath", "contentEvent", "errorEvent", "terminalEvent", "terminalLiteral", "reservedHeaders")));
            if (type.equals("SSE") != stream.mediaType().equals("text/event-stream")) throw invalid();
        }
        var headers = headers(fields.containsKey("headers") ? fields.get("headers") : Map.of());
        return new Step(id, method, path, body, headers, List.copyOf(dependsOn), guard, captures, responsePath, stream);
    }

    private static Map<String, String> headers(Object value) {
        if (!(value instanceof Map<?, ?> raw)) throw invalid();
        var headers = new LinkedHashMap<String, String>();
        for (var entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String name) || !(entry.getValue() instanceof String text)
                    || !Set.of("content-language", "accept-language").contains(name.toLowerCase(Locale.ROOT))
                    || text.contains("{{") || text.contains("}}") || !StandardCharsets.ISO_8859_1.newEncoder().canEncode(text)) throw invalid();
            headers.put(name, text);
        }
        TargetRules.headers(headers, null);
        return Map.copyOf(headers);
    }

    private static Guard guard(Object value, List<InteractionSession.Capture> available) {
        var fields = object(value, Set.of("name", "operator"), Set.of("value"));
        String name = string(fields, "name", 32);
        String operator = string(fields, "operator", 6);
        if (!Set.of("exists", "equals").contains(operator) || operator.equals("equals") != fields.containsKey("value")) throw invalid();
        var capture = available.stream().filter(c -> c.name().equals(name) && !c.sensitive()).findFirst().orElseThrow(LocalWorkflowBindings::invalid);
        Object expected = fields.get("value");
        if (operator.equals("equals") && !(switch (capture.type()) {
            case STRING -> expected instanceof String text && TargetRules.bytes(text) <= capture.maxBytes();
            case INTEGER -> expected instanceof Integer || expected instanceof Long || expected instanceof java.math.BigInteger;
            case BOOLEAN -> expected instanceof Boolean;
        })) throw invalid();
        return new Guard(name, operator, expected);
    }

    private static int promptCount(Object value) {
        if (value instanceof Map<?, ?> map) return map.values().stream().mapToInt(LocalWorkflowBindings::promptCount).sum();
        if (value instanceof List<?> list) return list.stream().mapToInt(LocalWorkflowBindings::promptCount).sum();
        return TargetSettings.PROMPT_PLACEHOLDER.equals(value) ? 1 : 0;
    }

    private NormalizedInteraction execute(TargetConfiguration target, String prompt, ScanBudget budget, Workflow workflow) {
        TargetUrl base = target.settings().url();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(target.settings().timeoutMs());
        long latencyMs = 0;
        var limits = transport.workflowBudget();
        var completed = new HashSet<String>();
        try (var session = new InteractionSession(base)) {
            for (int i = 0; i < workflow.steps().size(); i++) {
                var step = workflow.steps().get(i);
                boolean terminal = i == workflow.steps().size() - 1;
                long started = System.nanoTime();
                var metrics = new HttpTargetInvocationAdapter.StepMetrics();
                InvocationResult result;
                long preparationMs;
                try {
                    checkDeadline(budget, deadline);
                    if (!completed.containsAll(step.dependsOn())) throw new InvocationFailure(ExecutionError.TARGET_CONFIGURATION_INVALID);
                    if (step.guard() != null && !step.guard().allows(session, base)) {
                        latencyMs += elapsed(started);
                        log(budget, step, i, started, "SKIPPED", null, metrics);
                        continue;
                    }
                    TargetUrl destination = destination(base, resolvePath(step.path(), session, base));
                    var exchange = new HttpTargetInvocationAdapter.SessionExchange(step.method(), destination, deadline,
                            terminal ? null : (body, response) -> session.captureJson(transport.parseSessionResponse(body),
                                    step.captures()), session);
                    preparationMs = elapsed(started);
                    result = transport.invokeWorkflow(target, prompt, budget, step.body(), step.responsePath(),
                            step.stream(), exchange, new HttpTargetInvocationAdapter.WorkflowExchange(limits, step.headers(), metrics));
                } catch (InvocationFailure error) {
                    preparationMs = elapsed(started);
                    result = InvocationResult.failure(error.reason);
                } catch (IllegalArgumentException | TargetValidationException error) {
                    preparationMs = elapsed(started);
                    result = InvocationResult.failure(ExecutionError.TARGET_CONFIGURATION_INVALID);
                }
                // El transporte mide el intento después de adquirir la cuota.
                latencyMs += preparationMs + (result.latencyMs() == null ? 0 : result.latencyMs());
                log(budget, step, i, started, result.errorReason() == null ? "COMPLETED" : "FAILED", result, metrics);
                if (terminal || result.errorReason() != null) {
                    return LegacyInvocationResultProjection.fromLegacy(new InvocationResult(terminal ? result.httpStatus() : null, latencyMs,
                            result.errorReason(), result.requestBody(), result.responseText(), result.requestRedacted(), result.responseRedacted()));
                }
                completed.add(step.id());
            }
        }
        return LegacyInvocationResultProjection.fromLegacy(InvocationResult.failure(ExecutionError.TARGET_CONFIGURATION_INVALID));
    }

    private static void checkDeadline(ScanBudget budget, long deadline) {
        if (Thread.currentThread().isInterrupted()) throw new InvocationFailure(ExecutionError.EXECUTION_INTERRUPTED);
        if (budget.remainingNanos() == 0) throw new InvocationFailure(ExecutionError.SCAN_BUDGET_EXCEEDED);
        if (System.nanoTime() >= deadline) throw new InvocationFailure(ExecutionError.TARGET_TIMEOUT);
    }

    private static void log(ScanBudget budget, Step step, int index, long started, String state,
            InvocationResult result, HttpTargetInvocationAdapter.StepMetrics metrics) {
        LOG.debug("testExecution={} workflowStep={} ordinal={} state={} durationMs={} httpStatus={} error={} bytes={} units={} requestRedacted={} responseRedacted={}",
                budget.executionId(), step.id(), index + 1, state, elapsed(started),
                result == null ? null : result.httpStatus(), result == null ? null : result.errorReason(),
                metrics.bytes, metrics.units, result != null && result.requestRedacted(), result != null && result.responseRedacted());
    }

    private static long elapsed(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private static String identifier(Map<?, ?> fields, String key) {
        String id = string(fields, key, 32);
        if (!id.matches("[A-Za-z][A-Za-z0-9_-]{0,31}")) throw invalid();
        return id;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Binding de workflow no válido.");
    }
}
