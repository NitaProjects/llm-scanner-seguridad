package com.llmscannerseguridad.adapters.in.rest.scan;

import java.nio.file.*;
import java.lang.reflect.RecordComponent;
import java.util.*;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.domain.scan.Scan;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import static org.assertj.core.api.Assertions.*;

class EvaluationOpenApiContractTest {
    @SuppressWarnings("unchecked")
    private Map<String,Object> object(Object value) { return (Map<String,Object>)value; }
    @Test void openApiIsValidYamlAndKeepsExactPublicDtosAndReadOnlyEndpoints() throws Exception {
        var options=new LoaderOptions();options.setAllowDuplicateKeys(false);
        Map<String,Object> document;
        try(var input=Files.newInputStream(Path.of("../docs/05-api/openapi.yaml"))) {
            document=object(new Yaml(new SafeConstructor(options)).load(input));
        }
        var schemas=object(object(document.get("components")).get("schemas"));
        for(var entry:Map.of("EvaluationListing",EvaluationListing.class,"EvaluationMetadata",EvaluationMetadata.class,
                "FindingMetadata",FindingMetadata.class,"Scan",Scan.class).entrySet()) {
            var schema=object(schemas.get(entry.getKey()));
            var fields=Arrays.stream(entry.getValue().getRecordComponents()).map(RecordComponent::getName).toArray(String[]::new);
            assertThat(object(schema.get("properties")).keySet()).containsExactlyInAnyOrder(fields);
            assertThat((List<String>)schema.get("required")).containsExactlyInAnyOrder(fields);
            assertThat(schema.get("additionalProperties")).isEqualTo(false);
        }
        var paths=object(document.get("paths"));
        for(String suffix:List.of("evaluations","findings")) {
            var path=object(paths.get("/api/projects/{projectId}/scans/{scanId}/"+suffix));
            assertThat(path.keySet()).containsExactlyInAnyOrder("parameters","get");
            var responses=object(object(path.get("get")).get("responses"));
            assertThat(responses.keySet()).containsExactlyInAnyOrder("200","400","404","500");
            for(var response:responses.values()) {
                var header=object(object(object(response).get("headers")).get("Cache-Control"));
                assertThat(object(header.get("schema")).get("const")).isEqualTo("no-store");
            }
        }
    }
}
