package com.llmscannerseguridad.adapters.out.validation.target;

import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

public final class StrictTargetJson {
    private final ObjectReader reader = JsonMapper.builder(
            JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
            .build().readerFor(Object.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public Object read(String value) throws JacksonException {
        return reader.readValue(value);
    }
}
