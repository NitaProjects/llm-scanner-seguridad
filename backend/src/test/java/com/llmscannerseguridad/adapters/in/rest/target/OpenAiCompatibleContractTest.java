package com.llmscannerseguridad.adapters.in.rest.target;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import static org.assertj.core.api.Assertions.*;

class OpenAiCompatibleContractTest {
    @Test void openApiHasDiscriminatedBranchesAndConditionalConflictResponse() throws Exception {
        Map<?,?> document=new Yaml().load(Files.readString(Path.of("../docs/05-api/openapi.yaml")));
        var schemas=(Map<?,?>)((Map<?,?>)document.get("components")).get("schemas");
        assertThat((List<?>)((Map<?,?>)schemas.get("TargetConfigurationInput")).get("oneOf")).hasSize(2);
        assertThat((List<?>)((Map<?,?>)schemas.get("TargetConfiguration")).get("oneOf")).hasSize(2);
        var chat=(Map<?,?>)schemas.get("ChatTargetInput");assertThat(chat.get("additionalProperties")).isEqualTo(false);
        var properties=(Map<?,?>)chat.get("properties");
        assertThat(properties.keySet().stream().map(Object::toString).toList())
                .containsExactlyInAnyOrder("adapterType","name","endpoint","model","systemPrompt","authType","authConfig","timeoutMs");
        var route=(Map<?,?>)((Map<?,?>)document.get("paths")).get("/api/projects/{projectId}/target");
        assertThat(((Map<?,?>)((Map<?,?>)route.get("put")).get("responses")).containsKey("409")).isTrue();
        var legacy=(Map<?,?>)schemas.get("LegacyTargetConfiguration");
        assertThat((Map<?,?>)legacy.get("properties")).hasSize(16);
    }
}
