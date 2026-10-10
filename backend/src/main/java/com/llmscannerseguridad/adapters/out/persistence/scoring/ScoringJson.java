package com.llmscannerseguridad.adapters.out.persistence.scoring;

import java.util.List;
import com.llmscannerseguridad.domain.scoring.CategoryScore;
import com.llmscannerseguridad.domain.scoring.SeverityCounts;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

final class ScoringJson {
    private static final JsonMapper MAPPER = JsonMapper.builder(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(8).maxStringLength(16384).build()).build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT).build();
    private ScoringJson() { }
    static String write(Object value) { return MAPPER.writeValueAsString(value); }
    static SeverityCounts severities(String json) { return MAPPER.readValue(json, SeverityCounts.class); }
    static List<CategoryScore> categories(String json) { return List.of(MAPPER.readValue(json, CategoryScore[].class)); }
}
