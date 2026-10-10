package com.llmscannerseguridad.adapters.out.evaluation;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

public final class EvaluationJson {
    public static final JsonMapper MAPPER = JsonMapper.builder(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(32).maxStringLength(262144).build()).build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
                    DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT).build();
    private EvaluationJson() { }
    public static Object parse(String text) {
        var value = MAPPER.readValue(text, Object.class);
        unicode(value);
        return value;
    }
    public static String read(InputStream input, int maximumBytes) {
        try {
            byte[] bytes = input.readNBytes(maximumBytes + 1);
            if (bytes.length > maximumBytes) throw invalid();
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (Exception error) { throw invalid(); }
    }
    public static String resource(String name, int maximum) {
        try (var input = EvaluationJson.class.getResourceAsStream("/evaluation/" + name)) {
            if (input == null) throw invalid();
            return read(input, maximum);
        } catch (Exception error) { throw invalid(); }
    }
    public static String write(Object value) { return MAPPER.writeValueAsString(value); }
    public static String canonical(Object value) {
        return write(sorted(parse(write(value))));
    }
    private static Object sorted(Object value) {
        if (value instanceof Map<?, ?> map) {
            var copy = new TreeMap<String, Object>();
            map.forEach((key, item) -> copy.put((String) key, sorted(item)));
            return copy;
        }
        if (value instanceof List<?> list) return list.stream().map(EvaluationJson::sorted).toList();
        return value;
    }
    public static String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception error) { throw invalid(); }
    }
    public static int bytes(String text) { return text == null ? 0 : text.getBytes(StandardCharsets.UTF_8).length; }
    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value, Set<String> fields) {
        if (!(value instanceof Map<?, ?> map) || !fields.equals(map.keySet())) throw invalid();
        return (Map<String, Object>) map;
    }
    public static String string(Object value) {
        if (!(value instanceof String text)) throw invalid();
        return text;
    }
    public static int integer(Object value) {
        if (!(value instanceof java.math.BigInteger number)) throw invalid();
        try { return number.intValueExact(); } catch (Exception error) { throw invalid(); }
    }
    public static void unicode(Object value) {
        if (value instanceof String text && SecurityTest.invalidUnicode(text)) throw invalid();
        if (value instanceof Map<?, ?> map) map.forEach((key, item) -> { unicode(key); unicode(item); });
        if (value instanceof List<?> list) list.forEach(EvaluationJson::unicode);
    }
    public static IllegalArgumentException invalid() { return new IllegalArgumentException("Configuración o JSON de evaluación no válido."); }
}
