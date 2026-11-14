package com.llmscannerseguridad.adapters.out.http.target;

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
    private boolean closed;

    InteractionSession(TargetUrl origin) { this.origin = origin; }

    void capture(Object json, ClassicHttpResponse response, List<Capture> declarations) {
        if (closed || !(json instanceof Map<?, ?>)) throw malformed();
        for (Capture declaration : declarations) {
            Value value = switch (declaration.source()) {
                case JSON -> jsonValue(declaration, json);
                case HEADER -> header(declaration, response);
                case COOKIE -> cookie(declaration, response);
            };
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
                        other.sensitive() && value.data().toString().contains(other.data().toString()))) {
            throw new InvocationFailure(ExecutionError.TARGET_CONFIGURATION_INVALID);
        }
        return value.data();
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
        if (text.isBlank() || TargetRules.controls(text)) throw malformed();
        if (TargetRules.bytes(text) > declaration.maxBytes()) throw new InvocationFailure(ExecutionError.RESPONSE_TOO_LARGE);
        return new Value(data, declaration.source(), origin.origin(), declaration.sensitive(), attributes);
    }

    UnaryOperator<String> redactor(String credential) {
        var secrets = new HashSet<String>();
        addSecret(secrets, credential);
        for (Value value : values.values()) {
            if (value.sensitive()) addSecret(secrets, value.data().toString());
        }
        return text -> redactMatches(text, secrets);
    }

    private static void addSecret(Set<String> secrets, String raw) {
        if (raw == null || raw.isEmpty()) return;
        secrets.add(raw);
        String encoded = tools.jackson.databind.json.JsonMapper.shared().writeValueAsString(raw);
        secrets.add(encoded.substring(1, encoded.length() - 1));
    }

    private static String redactMatches(String text, Set<String> secrets) {
        int[] ends = null;
        for (String secret : secrets) {
            for (int start = text.indexOf(secret); start >= 0; start = text.indexOf(secret, start + 1)) {
                if (ends == null) ends = new int[text.length()];
                ends[start] = Math.max(ends[start], start + secret.length());
            }
        }
        if (ends == null) return text;
        var result = new StringBuilder(text.length());
        int copied = 0;
        for (int start = 0; start < ends.length; start++) {
            if (ends[start] == 0) continue;
            int end = ends[start];
            for (int next = start + 1; next <= end && next < ends.length; next++) {
                end = Math.max(end, ends[next]);
            }
            result.append(text, copied, start).append("[REDACTED_CREDENTIAL]");
            copied = end;
            start = end - 1;
        }
        return result.append(text, copied, text.length()).toString();
    }

    @Override public void close() { values.clear(); closed = true; }
    private static InvocationFailure malformed() { return new InvocationFailure(ExecutionError.RESPONSE_FORMAT_ERROR); }
}
