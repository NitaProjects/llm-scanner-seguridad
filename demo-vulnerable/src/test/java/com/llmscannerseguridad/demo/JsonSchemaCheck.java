package com.llmscannerseguridad.demo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

// Subconjunto explícito usado por los esquemas locales; sin dependencia de runtime adicional.
final class JsonSchemaCheck {
    private JsonSchemaCheck() { }
    static void validate(Object value, Object schemaValue) {
        var schema = TestJson.object(schemaValue);
        if (schema.containsKey("const")) assertThat(value).isEqualTo(schema.get("const"));
        if (schema.containsKey("enum")) assertThat(((List<?>) schema.get("enum")).contains(value)).isTrue();
        if (schema.containsKey("anyOf")) {
            boolean matched = false;
            for (Object option : (List<?>) schema.get("anyOf")) {
                try { validate(value, option); matched = true; break; } catch (AssertionError ignored) { }
            }
            assertThat(matched).isTrue();
        }
        if (schema.containsKey("type")) {
            Object type = schema.get("type");
            var types = type instanceof List<?> list ? list : List.of(type);
            assertThat(types.stream().anyMatch(t -> matches(value, (String) t))).isTrue();
        }
        if (value instanceof Map<?, ?> map && schema.containsKey("properties")) {
            var properties = TestJson.object(schema.get("properties"));
            for (Object required : (List<?>) schema.getOrDefault("required", List.of())) assertThat(map.containsKey(required)).isTrue();
            if (Boolean.FALSE.equals(schema.get("additionalProperties"))) assertThat(map.keySet()).isEqualTo(properties.keySet());
            map.forEach((key, item) -> { if (properties.containsKey(key)) validate(item, properties.get(key)); });
        }
        if (value instanceof List<?> list) {
            if (schema.containsKey("minItems")) assertThat(list.size()).isGreaterThanOrEqualTo((int) schema.get("minItems"));
            if (schema.containsKey("maxItems")) assertThat(list.size()).isLessThanOrEqualTo((int) schema.get("maxItems"));
            if (schema.containsKey("items")) list.forEach(item -> validate(item, schema.get("items")));
        }
        if (value instanceof String text) {
            if (schema.containsKey("minLength")) assertThat(text.codePointCount(0, text.length())).isGreaterThanOrEqualTo((int) schema.get("minLength"));
            if (schema.containsKey("pattern")) assertThat(text).containsPattern((String) schema.get("pattern"));
            if ("uuid".equals(schema.get("format"))) {
                assertThatCode(() -> assertThat(UUID.fromString(text).toString()).isEqualTo(text)).doesNotThrowAnyException();
            }
            if ("date-time".equals(schema.get("format"))) {
                assertThatCode(() -> assertThat(Instant.parse(text).getNano() % 1000).isZero()).doesNotThrowAnyException();
            }
        }
    }
    private static boolean matches(Object value, String type) {
        return switch (type) {
            case "null" -> value == null;
            case "object" -> value instanceof Map;
            case "array" -> value instanceof List;
            case "string" -> value instanceof String;
            case "boolean" -> value instanceof Boolean;
            case "integer" -> value instanceof Integer || value instanceof Long;
            default -> throw new IllegalArgumentException("Tipo de esquema no soportado");
        };
    }
}
