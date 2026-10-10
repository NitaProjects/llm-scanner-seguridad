package com.llmscannerseguridad.adapters.reporting;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.UUID;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

public final class ReportJson {
    public static final int MAX_BYTES = 262144;
    public static final int WRAPPER_BYTES = 512;
    private static final DateTimeFormatter UTC = new DateTimeFormatterBuilder().appendInstant(6).toFormatter();
    private static final JsonMapper MAPPER = mapper();
    private ReportJson() { }
    private static JsonMapper mapper() {
        var types = new SimpleModule("report-v1-types");
        types.addSerializer(Instant.class,new ValueSerializer<Instant>() {
            @Override public void serialize(Instant value, JsonGenerator generator, SerializationContext context) {
                generator.writeString(UTC.format(value));
            }
        });
        types.addDeserializer(Instant.class,new ValueDeserializer<Instant>() {
            @Override public Instant deserialize(JsonParser parser, DeserializationContext context) {
                var text = parser.getString();
                if (!parser.hasToken(JsonToken.VALUE_STRING) || !text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{6}Z")) throw ScanOperationException.internal();
                var value = Instant.parse(text);
                if (!UTC.format(value).equals(text)) throw ScanOperationException.internal();
                return value;
            }
        });
        types.addDeserializer(UUID.class,new ValueDeserializer<UUID>() {
            @Override public UUID deserialize(JsonParser parser, DeserializationContext context) {
                var text = parser.getString();
                if (!parser.hasToken(JsonToken.VALUE_STRING) || !text.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) throw ScanOperationException.internal();
                return UUID.fromString(text);
            }
        });
        return JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(12).maxStringLength(MAX_BYTES).build()).build())
                .addModule(types).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT).build();
    }
    public static void guard(int bytes, int maximum) {
        if (bytes < 0 || bytes > maximum) throw ScanOperationException.internal();
    }
    public static String write(Object value) {
        try {
            var json = MAPPER.writeValueAsString(value);
            guard(json.getBytes(StandardCharsets.UTF_8).length,MAX_BYTES); return json;
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    public static <T> T read(String json, Class<T> type) {
        try {
            guard(json.getBytes(StandardCharsets.UTF_8).length,MAX_BYTES);
            return MAPPER.readValue(json,type);
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    public static String response(Object wrapper, Object report) {
        try {
            int reportBytes = report == null ? 4 : write(report).getBytes(StandardCharsets.UTF_8).length;
            var json = MAPPER.writeValueAsString(wrapper); int bytes = json.getBytes(StandardCharsets.UTF_8).length;
            guard(bytes-reportBytes,WRAPPER_BYTES); guard(bytes,MAX_BYTES+WRAPPER_BYTES); return json;
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
}
