package com.llmscannerseguridad.adapters.out.http.target;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.TargetRules;
import com.llmscannerseguridad.domain.target.TargetUrl;
import org.apache.hc.core5.http.ClassicHttpResponse;

/** Un almacén por prueba, con procedencia fija y sin representación de sus valores en logs. */
final class InteractionSession implements AutoCloseable {
    enum Source { JSON, HEADER, COOKIE }
    enum Type { STRING, INTEGER, BOOLEAN }
    record Capture(String name, Source source, String selector, List<Object> path, Type type,
            int maxBytes, boolean required, boolean sensitive) { }
    record Value(Object data, Source source, String origin, boolean sensitive, Map<String, String> cookieAttributes) {
        @Override public String toString() { return "SessionValue[protegido]"; }
    }

    private final TargetUrl origin;
    private final Map<String, Value> values = new HashMap<>();
    private final Map<Capture, Map<String, WorkflowCookie>> workflowCookies = new LinkedHashMap<>();
    private final Set<String> retiredCookieValues = new HashSet<>();
    private final Set<String> forwardedHeaderValues = new HashSet<>();
    private boolean closed;

    InteractionSession(TargetUrl origin) { this.origin = origin; }

    void capture(Object json, ClassicHttpResponse response, List<Capture> declarations) {
        if (!(json instanceof Map<?, ?>)) throw malformed();
        captureValues(json, response, declarations);
    }

    void captureWorkflow(Object json, ClassicHttpResponse response, List<Capture> declarations, TargetUrl source) {
        if (closed || !origin.sameOrigin(source)) throw malformed();
        URI uri = URI.create(source.value());
        Instant now = Instant.now();
        // Un Set-Cookie posterior puede invalidar un handle, pero nunca autoriza uno nuevo.
        for (var entry : workflowCookies.entrySet()) {
            var seen = new HashSet<String>();
            for (var header : response.getHeaders("Set-Cookie")) {
                var cookie = WorkflowCookie.parse(header.getValue(), entry.getKey().selector(), uri, now, entry.getKey().maxBytes());
                if (cookie != null) {
                    if (!seen.add(cookie.path())) throw malformed();
                    var previous = entry.getValue().remove(cookie.path());
                    if (previous != null) retiredCookieValues.add(previous.value());
                    retiredCookieValues.add(cookie.value());
                }
            }
        }
        for (var capture : declarations) {
            if (capture.source() != Source.COOKIE) continue;
            var cookies = new LinkedHashMap<String, WorkflowCookie>();
            for (var header : response.getHeaders("Set-Cookie")) {
                var cookie = WorkflowCookie.parse(header.getValue(), capture.selector(), uri, now, capture.maxBytes());
                if (cookie != null && (cookies.size() >= 16 || cookies.putIfAbsent(cookie.path(), cookie) != null)) throw malformed();
            }
            if (cookies.isEmpty() && capture.required()) throw new InvocationFailure(ExecutionError.RESPONSE_PATH_NOT_FOUND);
            workflowCookies.put(capture, cookies);
        }
        captureValues(json, response, declarations.stream().filter(capture -> capture.source() != Source.COOKIE).toList());
    }

    private void captureValues(Object json, ClassicHttpResponse response, List<Capture> declarations) {
        if (closed) throw malformed();
        for (Capture declaration : declarations) {
            Value value;
            try {
                value = switch (declaration.source()) {
                    case JSON -> jsonValue(declaration, json);
                    case HEADER -> header(declaration, response);
                    case COOKIE -> cookie(declaration, response);
                };
            } catch (RuntimeException failure) {
                if (declaration.sensitive()) com.llmscannerseguridad.privacy.ProtectionScope.incomplete();
                throw failure;
            }
            if (value == null) {
                if (declaration.required()) throw new InvocationFailure(ExecutionError.RESPONSE_PATH_NOT_FOUND);
            } else if (values.putIfAbsent(declaration.name(), value) != null) throw malformed();
        }
    }

    Value value(String name) { return values.get(name); }

    Object identifier(String name, TargetUrl destination) {
        Value value = values.get(name);
        if (closed || value == null || value.sensitive() || value.source() != Source.JSON
                || !origin.sameOrigin(destination) || values.values().stream().anyMatch(other ->
                        other.sensitive() && value.data().toString().contains(other.data().toString()))
                || workflowCookieSecrets().stream().anyMatch(secret -> !secret.isEmpty() && value.data().toString().contains(secret))) {
            throw new InvocationFailure(ExecutionError.TARGET_CONFIGURATION_INVALID);
        }
        return value.data();
    }

    String credential(Capture capture, TargetUrl destination, Instant now) {
        if (closed || !capture.sensitive() || !origin.sameOrigin(destination)) throw unavailable();
        if (capture.source() == Source.COOKIE) {
            var cookies = workflowCookies.getOrDefault(capture, Map.of()).values().stream()
                    .filter(cookie -> cookie.applies(URI.create(destination.value()), now)).toList();
            if (cookies.size() != 1) throw unavailable();
            return cookies.getFirst().value();
        }
        Value value = values.get(capture.name());
        if (value == null || !value.origin().equals(destination.origin())) throw unavailable();
        String text = value.data().toString();
        com.llmscannerseguridad.privacy.ProtectionScope.register(text);
        com.llmscannerseguridad.privacy.ProtectionScope.register(text.strip());
        forwardedHeaderValues.add(text);
        forwardedHeaderValues.add(text.strip());
        return text;
    }

    private Set<String> workflowCookieSecrets() {
        var secrets = new HashSet<>(retiredCookieValues);
        workflowCookies.values().forEach(cookies -> cookies.values().forEach(cookie -> secrets.add(cookie.value())));
        return secrets;
    }

    private Value jsonValue(Capture capture, Object node) {
        for (Object segment : capture.path()) {
            if (segment instanceof String key && node instanceof Map<?, ?> map && map.containsKey(key)) node = map.get(key);
            else if (segment instanceof Integer index && node instanceof List<?> list && index < list.size()) node = list.get(index);
            else return null;
        }
        boolean valid = switch (capture.type()) {
            case STRING -> node instanceof String;
            case INTEGER -> node instanceof java.math.BigInteger;
            case BOOLEAN -> node instanceof Boolean;
        };
        if (!valid) throw malformed();
        return value(capture, node, Map.of());
    }

    private Value header(Capture capture, ClassicHttpResponse response) {
        var headers = response.getHeaders(capture.selector());
        if (headers.length > 1) throw malformed();
        return headers.length == 0 ? null : value(capture, headers[0].getValue(), Map.of());
    }

    private Value cookie(Capture capture, ClassicHttpResponse response) {
        Value found = null;
        for (var header : response.getHeaders("Set-Cookie")) {
            String[] parts = header.getValue().split(";", -1);
            int separator = parts[0].indexOf('=');
            if (separator <= 0 || !parts[0].substring(0, separator).strip().equals(capture.selector())) continue;
            if (found != null || parts.length > 17) throw malformed();
            var attributes = new LinkedHashMap<String, String>();
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].strip();
                int equals = part.indexOf('=');
                String name = (equals < 0 ? part : part.substring(0, equals)).toLowerCase(Locale.ROOT);
                String data = equals < 0 ? "" : part.substring(equals + 1);
                if (!name.matches("[a-z][a-z0-9-]{0,31}") || data.length() > 1024 || TargetRules.controls(data)
                        || attributes.putIfAbsent(name, data) != null) throw malformed();
            }
            String token = parts[0].substring(separator + 1).strip();
            if (token.length() >= 2 && token.startsWith("\"") && token.endsWith("\"")) token = token.substring(1, token.length() - 1);
            if (!token.matches("[\\x21\\x23-\\x2B\\x2D-\\x3A\\x3C-\\x5B\\x5D-\\x7E]+")) throw malformed();
            found = value(capture, token, Map.copyOf(attributes));
        }
        return found;
    }

    private Value value(Capture declaration, Object data, Map<String, String> attributes) {
        String text = data.toString();
        if (declaration.sensitive()) com.llmscannerseguridad.privacy.ProtectionScope.register(text);
        if (text.isBlank() || TargetRules.controls(text)) throw malformed();
        if (TargetRules.bytes(text) > declaration.maxBytes()) throw new InvocationFailure(ExecutionError.RESPONSE_TOO_LARGE);
        return new Value(data, declaration.source(), origin.origin(), declaration.sensitive(), attributes);
    }

    UnaryOperator<String> redactor(String credential) {
        return redactor(credential, Set.of());
    }

    UnaryOperator<String> redactor(String credential, Set<String> additional) {
        var secrets = new HashSet<String>();
        addSecret(secrets, credential);
        additional.forEach(value -> addSecret(secrets, value));
        for (Value value : values.values()) {
            if (value.sensitive()) {
                String text = value.data().toString();
                addSecret(secrets, text);
                if (value.source() == Source.HEADER) addSecret(secrets, text.strip());
            }
        }
        workflowCookieSecrets().forEach(secret -> addSecret(secrets, secret));
        forwardedHeaderValues.forEach(secret -> addSecret(secrets, secret));
        return text -> redactMatches(text, secrets);
    }

    static UnaryOperator<String> credentialRedactor(String credential) {
        return credentialRedactor(credential, Set.of());
    }

    static UnaryOperator<String> credentialRedactor(String credential, Set<String> additional) {
        var secrets = new HashSet<String>();
        addSecret(secrets, credential);
        additional.forEach(value -> addSecret(secrets, value));
        return text -> redactMatches(text, secrets);
    }

    private static void addSecret(Set<String> secrets, String raw) {
        if (raw == null || raw.isEmpty()) return;
        secrets.add(raw);
        com.llmscannerseguridad.privacy.ProtectionScope.register(raw);
    }

    private static String redactMatches(String text, Set<String> secrets) {
        try { return com.llmscannerseguridad.privacy.DurableEvidenceProtection.redact(text, secrets, 262144); }
        catch (RuntimeException error) { throw new InvocationFailure(ExecutionError.EVIDENCE_LIMIT_EXCEEDED); }
    }

    @Override public void close() {
        values.clear();
        workflowCookies.clear();
        retiredCookieValues.clear();
        forwardedHeaderValues.clear();
        closed = true;
    }
    private static InvocationFailure unavailable() { return new InvocationFailure(ExecutionError.CREDENTIAL_UNAVAILABLE); }
    private static InvocationFailure malformed() { return new InvocationFailure(ExecutionError.RESPONSE_FORMAT_ERROR); }
}
