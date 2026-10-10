package com.llmscannerseguridad.adapters.out.validation.target;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;
import com.llmscannerseguridad.domain.target.TargetSettings;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class TargetJsonProcessor {
    private static final String REDACTED = "[REDACTED_CREDENTIAL]";
    private final JsonTargetConfigurationValidator validator;
    private final JsonMapper json = mapper(64);
    private final JsonMapper requestJson = mapper(1000);

    private static JsonMapper mapper(int depth) {
        return JsonMapper.builder(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(depth).build()).build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                    DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
                    DeserializationFeature.USE_BIG_INTEGER_FOR_INTS).build();
    }

    public TargetJsonProcessor(JsonTargetConfigurationValidator validator) {
        this.validator = validator;
    }

    public String render(TargetSettings settings, String prompt) {
        validator.template(settings.requestTemplate());
        validator.responsePath(settings.responsePath());
        Object prepared = replace(requestJson.readValue(settings.requestTemplate(), Object.class), prompt);
        unicode(prepared);
        return requestJson.writeValueAsString(prepared);
    }

    private Object replace(Object value, String prompt) {
        if (value instanceof String text) {
            return text.equals(TargetSettings.PROMPT_PLACEHOLDER) ? prompt : text;
        }
        if (value instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            map.forEach((key, item) -> copy.put((String) key, replace(item, prompt)));
            return copy;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(item -> replace(item, prompt)).toList();
        }
        return value;
    }

    public String extract(String body, String path) {
        Object value;
        try {
            value = json.readValue(body, Object.class);
            unicode(value);
        } catch (RuntimeException error) {
            throw new JsonFailure(ExecutionError.RESPONSE_INVALID_JSON);
        }
        for (Object segment : validator.pathSegments(path)) {
            if (segment instanceof String key && value instanceof Map<?, ?> map && map.containsKey(key)) {
                value = map.get(key);
            } else if (segment instanceof Integer index && value instanceof List<?> list && index < list.size()) {
                value = list.get(index);
            } else {
                throw new JsonFailure(ExecutionError.RESPONSE_PATH_NOT_FOUND);
            }
        }
        if (!(value instanceof String text)) {
            throw new JsonFailure(ExecutionError.RESPONSE_NOT_TEXT);
        }
        return text;
    }

    private void unicode(Object value) {
        if (value instanceof String text && SecurityTest.invalidUnicode(text)) {
            throw new IllegalArgumentException("Unicode no válido.");
        } else if (value instanceof Map<?, ?> map) {
            map.forEach((key, item) -> { unicode(key); unicode(item); });
        } else if (value instanceof List<?> list) {
            list.forEach(this::unicode);
        }
    }

    public Evidence redact(String request, String response, String secret) {
        String safeRequest;
        try {
            safeRequest = requestJson.writeValueAsString(redactNode(requestJson.readValue(request, Object.class), secret));
        } catch (RuntimeException error) {
            throw new JsonFailure(ExecutionError.EVIDENCE_LIMIT_EXCEEDED);
        }
        String safeResponse = response == null ? null : redactText(response, secret);
        return new Evidence(safeRequest, safeResponse, !safeRequest.equals(request),
                response != null && !safeResponse.equals(response));
    }

    private Object redactNode(Object value, String secret) {
        if (value instanceof String text) {
            return redactText(text, secret);
        }
        if (value instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) {
                String key = redactText((String) entry.getKey(), secret);
                if (copy.containsKey(key)) {
                    throw new JsonFailure(ExecutionError.EVIDENCE_LIMIT_EXCEEDED);
                }
                copy.put(key, redactNode(entry.getValue(), secret));
            }
            return copy;
        }
        if (value instanceof List<?> list) {
            var copy = new ArrayList<Object>();
            list.forEach(item -> copy.add(redactNode(item, secret)));
            return copy;
        }
        return value;
    }

    private String redactText(String value, String secret) {
        return secret == null ? value : value.replace(secret, REDACTED);
    }

    public int serializedTextBytes(String value) {
        return value == null ? 0 : json.writeValueAsBytes(value).length;
    }

    public record Evidence(String request, String response, boolean requestRedacted, boolean responseRedacted) {
        @Override
        public String toString() {
            return "Evidence[protegida]";
        }
    }

    public static final class JsonFailure extends RuntimeException {
        private final ExecutionError reason;

        public JsonFailure(ExecutionError reason) {
            super("No se ha podido procesar la evidencia técnica.", null, false, false);
            this.reason = reason;
        }

        public ExecutionError reason() {
            return reason;
        }
    }
}
