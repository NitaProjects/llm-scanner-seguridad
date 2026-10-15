package com.llmscannerseguridad.demo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class TestJson {
    public static final JsonMapper MAPPER = JsonMapper.builder(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private TestJson() { }
    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value) { return (Map<String, Object>) value; }
    public static Object read(String path) throws Exception { return MAPPER.readValue(Files.readString(Path.of(path)), Object.class); }
}
