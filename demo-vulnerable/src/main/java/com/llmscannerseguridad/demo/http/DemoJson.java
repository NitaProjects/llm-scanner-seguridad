package com.llmscannerseguridad.demo.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class DemoJson {
    public static final int BODY_BYTES = 8192;
    public static final int MESSAGE_BYTES = 4096;
    public static final int RESPONSE_BYTES = 4096;
    private static final JsonMapper MAPPER = JsonMapper.builder(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(4)
                    .maxStringLength(BODY_BYTES).build()).build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    private DemoJson() { }

    public static byte[] read(InputStream input, int maximum) throws IOException {
        byte[] bytes = input.readNBytes(maximum + 1);
        if (bytes.length > maximum) throw new RequestFailure(413, "REQUEST_TOO_LARGE");
        return bytes;
    }

    public static Object parse(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            Object value = MAPPER.readValue(text, Object.class);
            unicode(value);
            return value;
        } catch (Exception error) {
            throw new RequestFailure(400, "INVALID_REQUEST");
        }
    }

    public static String message(byte[] body) {
        Object value = parse(body);
        if (!(value instanceof Map<?, ?> map) || !map.keySet().equals(Set.of("message"))
                || !(map.get("message") instanceof String message) || message.isBlank()) {
            throw new RequestFailure(400, "INVALID_REQUEST");
        }
        if (message.getBytes(StandardCharsets.UTF_8).length > MESSAGE_BYTES) {
            throw new RequestFailure(413, "REQUEST_TOO_LARGE");
        }
        return message;
    }

    public static byte[] write(Object value) {
        try {
            unicode(value);
            return MAPPER.writeValueAsBytes(value);
        } catch (Exception error) {
            throw new RequestFailure(500, "INTERNAL_ERROR");
        }
    }

    public static byte[] response(Object value) {
        byte[] bytes = write(value);
        if (bytes.length > RESPONSE_BYTES) throw new RequestFailure(500, "INTERNAL_ERROR");
        return bytes;
    }

    private static void unicode(Object value) {
        if (value instanceof String text) {
            for (int i = 0; i < text.length(); i++) {
                char character = text.charAt(i);
                if (Character.isHighSurrogate(character)) {
                    if (++i == text.length() || !Character.isLowSurrogate(text.charAt(i))) throw new IllegalArgumentException();
                } else if (Character.isLowSurrogate(character)) throw new IllegalArgumentException();
            }
        } else if (value instanceof Map<?, ?> map) {
            map.forEach((key, item) -> { unicode(key); unicode(item); });
        } else if (value instanceof List<?> list) list.forEach(DemoJson::unicode);
    }
}
