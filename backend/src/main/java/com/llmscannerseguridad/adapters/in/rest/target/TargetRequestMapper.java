package com.llmscannerseguridad.adapters.in.rest.target;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.llmscannerseguridad.adapters.in.rest.project.InvalidProjectIdException;
import com.llmscannerseguridad.adapters.out.validation.target.StrictTargetJson;
import com.llmscannerseguridad.application.target.TargetCommand;
import com.llmscannerseguridad.domain.target.AuthType;
import com.llmscannerseguridad.domain.target.TargetRules;
import com.llmscannerseguridad.domain.target.TargetSettings;
import com.llmscannerseguridad.domain.target.TargetUrl;
import com.llmscannerseguridad.domain.target.TargetAdapterType;
import com.llmscannerseguridad.domain.target.TargetValidationException;
import tools.jackson.core.JacksonException;

public final class TargetRequestMapper {
    private static final Set<String> FIELDS = Set.of("adapterType", "name", "url", "method", "contentType", "requestTemplate",
            "promptPlaceholder", "responsePath", "headers", "authType", "authConfig", "timeoutMs");
    private static final Set<String> CHAT_FIELDS = Set.of("adapterType", "name", "endpoint", "model", "systemPrompt", "authType", "authConfig", "timeoutMs");
    private final StrictTargetJson json = new StrictTargetJson();

    public UUID identifier(String text) {
        if (text == null || !text.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new InvalidProjectIdException();
        }
        return UUID.fromString(text);
    }

    public TargetCommand command(String body) {
        Object parsed;
        try {
            if (body == null || body.isBlank()) {
                throw new InvalidTargetRequestException();
            }
            parsed = json.read(body);
        } catch (JacksonException error) {
            throw new InvalidTargetRequestException();
        }
        if (!(parsed instanceof Map<?, ?> fields)) {
            throw new InvalidTargetRequestException();
        }
        TargetAdapterType adapterType = TargetAdapterType.REST_LEGACY;
        if (fields.containsKey("adapterType")) {
            try { adapterType = TargetAdapterType.valueOf(text(fields, "adapterType", true)); }
            catch (IllegalArgumentException error) { throw TargetValidationException.field("adapterType", "NOT_ALLOWED"); }
        }
        boolean chat = adapterType == TargetAdapterType.OPENAI_COMPATIBLE;
        var permitted = chat ? CHAT_FIELDS : FIELDS;
        for (var entry : fields.entrySet()) {
            String field = (String) entry.getKey();
            // Solo rutas del contrato, nunca claves arbitrarias aportadas por el cliente.
            TargetRules.noNullCharacter(field, "url");
            rejectNullCharacters(entry.getValue(), permitted.contains(field) ? field : "url");
        }
        if (!permitted.containsAll(fields.keySet())) {
            if (chat) throw TargetValidationException.field("adapterType", "NOT_ALLOWED");
            throw new InvalidTargetRequestException();
        }
        constant(fields, "method", TargetSettings.METHOD);
        constant(fields, "contentType", TargetSettings.CONTENT_TYPE);
        constant(fields, "promptPlaceholder", TargetSettings.PROMPT_PLACEHOLDER);
        AuthType type;
        String authType = text(fields, "authType", true);
        try {
            type = AuthType.valueOf(authType);
        } catch (IllegalArgumentException error) {
            throw TargetValidationException.field("authType", "NOT_ALLOWED");
        }
        Map<?, ?> auth = Map.of();
        Object authObject = fields.get("authConfig");
        if (authObject instanceof Map<?, ?> map) {
            auth = map;
        }
        for (Object key : auth.keySet()) {
            TargetRules.noNullCharacter((String) key, "authConfig");
        }
        if (!Set.of("headerName", "secretValue").containsAll(auth.keySet())) {
            throw new InvalidTargetRequestException();
        }
        if (type == AuthType.NONE) {
            if (authObject != null) {
                throw TargetValidationException.field("authConfig", "NOT_ALLOWED");
            }
        } else {
            if (authObject == null) {
                throw TargetValidationException.field("authConfig", "REQUIRED");
            }
            if (!(authObject instanceof Map<?, ?>)) {
                throw TargetValidationException.field("authConfig", "INVALID_TYPE");
            }
        }
        String headerName = null;
        if (type == AuthType.API_KEY_HEADER) {
            headerName = text(auth, "headerName", true, "authConfig.headerName");
        } else if (auth.containsKey("headerName")) {
            throw TargetValidationException.field("authConfig.headerName", "NOT_ALLOWED");
        }
        boolean secretPresent = auth.containsKey("secretValue");
        String secret = secretPresent ? text(auth, "secretValue", true, "authConfig.secretValue") : null;
        var headers = new LinkedHashMap<String, String>();
        if (fields.containsKey("headers")) {
            if (!(fields.get("headers") instanceof Map<?, ?> map)) {
                throw TargetValidationException.field("headers", "INVALID_TYPE");
            }
            for (var entry : map.entrySet()) {
                String key = (String) entry.getKey();
                TargetRules.noNullCharacter(key, "headers");
                if (!(entry.getValue() instanceof String value)) {
                    throw TargetValidationException.field("headers", "INVALID_TYPE");
                }
                TargetRules.noNullCharacter(value, "headers");
                headers.put(key, value);
            }
        }
        int timeout = 10000;
        if (fields.containsKey("timeoutMs")) {
            Object value = fields.get("timeoutMs");
            if (!(value instanceof Byte || value instanceof Short || value instanceof Integer
                    || value instanceof Long || value instanceof BigInteger)) {
                throw TargetValidationException.field("timeoutMs", "INVALID_TYPE");
            }
            BigInteger number = new BigInteger(value.toString());
            if (number.compareTo(BigInteger.valueOf(1000)) < 0
                    || number.compareTo(BigInteger.valueOf(60000)) > 0) {
                throw TargetValidationException.field("timeoutMs", "OUT_OF_RANGE");
            }
            timeout = number.intValue();
        }
        var settings = new TargetSettings(text(fields, "name", false),
                new TargetUrl(text(fields, chat ? "endpoint" : "url", true)), chat ? null : text(fields, "requestTemplate", true),
                chat ? null : text(fields, "responsePath", true), headers, type, headerName, timeout, adapterType,
                chat ? text(fields, "model", true) : null, chat ? text(fields, "systemPrompt", false) : null);
        return new TargetCommand(settings, secret, secretPresent, fields.containsKey("adapterType"));
    }

    private static void constant(Map<?, ?> fields, String field, String expected) {
        if (fields.containsKey(field) && !expected.equals(text(fields, field, true))) {
            throw TargetValidationException.field(field, "NOT_ALLOWED");
        }
    }

    private static void rejectNullCharacters(Object value, String field) {
        if (value instanceof String text) {
            TargetRules.noNullCharacter(text, field);
        } else if (value instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                String key = (String) entry.getKey();
                TargetRules.noNullCharacter(key, field);
                String nested = field.equals("authConfig") && Set.of("headerName", "secretValue").contains(key)
                        ? field + "." + key : field;
                rejectNullCharacters(entry.getValue(), nested);
            }
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                rejectNullCharacters(item, field);
            }
        }
    }

    private static String text(Map<?, ?> fields, String key, boolean required) {
        return text(fields, key, required, key);
    }

    private static String text(Map<?, ?> fields, String key, boolean required, String field) {
        Object value = fields.get(key);
        if (value == null) {
            if (required) {
                throw TargetValidationException.field(field, "REQUIRED");
            }
            return null;
        }
        if (!(value instanceof String text)) {
            throw TargetValidationException.field(field, "INVALID_TYPE");
        }
        TargetRules.noNullCharacter(text, field);
        return text;
    }
}
