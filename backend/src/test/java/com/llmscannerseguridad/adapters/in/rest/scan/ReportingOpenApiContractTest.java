package com.llmscannerseguridad.adapters.in.rest.scan;

import java.nio.file.*;
import java.util.*;
import java.lang.reflect.RecordComponent;
import com.llmscannerseguridad.domain.reporting.*;
import com.llmscannerseguridad.domain.scan.Scan;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import static org.assertj.core.api.Assertions.*;

class ReportingOpenApiContractTest {
    @SuppressWarnings("unchecked") private Map<String,Object> object(Object v) { return (Map<String,Object>)v; }
    @Test @SuppressWarnings("unchecked") void exactClosedContractsReadOnlyEndpointNoStoreAndMinimalProjections() throws Exception {
        var options=new LoaderOptions();options.setAllowDuplicateKeys(false);Map<String,Object> doc;
        try(var input=Files.newInputStream(Path.of("../docs/05-api/openapi.yaml"))) {doc=object(new Yaml(new SafeConstructor(options)).load(input));}
        var schemas=object(object(doc.get("components")).get("schemas"));
        var records=Map.of("ReportResponse",ReportResponse.class,"SecurityReport",SecurityReport.class,"ReportScope",SecurityReport.Scope.class,
                "ReportEvaluation",ReportEvaluation.class,"ReportVersions",ReportEvaluation.Versions.class,"ReportFinding",ReportFinding.class,
                "ReportInterpretation",SecurityReport.Interpretation.class,"ReportLimitation",SecurityReport.Limitation.class);
        for(var entry:records.entrySet()) {
            var schema=object(schemas.get(entry.getKey()));var fields=Arrays.stream(entry.getValue().getRecordComponents()).map(RecordComponent::getName).toArray(String[]::new);
            assertThat(object(schema.get("properties")).keySet()).containsExactlyInAnyOrder(fields);assertThat((List<String>)schema.get("required")).containsExactlyInAnyOrder(fields);assertThat(schema.get("additionalProperties")).isEqualTo(false);
        }
        var path=object(object(doc.get("paths")).get("/api/projects/{projectId}/scans/{scanId}/report"));assertThat(path.keySet()).containsExactlyInAnyOrder("parameters","get");
        for(var response:object(object(path.get("get")).get("responses")).values()) assertThat(object(object(object(object(response).get("headers")).get("Cache-Control")).get("schema")).get("const")).isEqualTo("no-store");
        assertThat(Scan.class.getRecordComponents()).hasSize(7);
        var sql=Files.readString(Path.of("src/main/java/com/llmscannerseguridad/adapters/out/persistence/reporting/PostgresReportInputAdapter.java"));
        assertThat(sql).doesNotContain("select *","llm_assessment","deterministic_signals","configuration_snapshot","evidence_projection","response_payload","request_payload","target_configurations","credential");
        for(String dir:List.of("application/reporting","domain/reporting")) try(var files=Files.list(Path.of("src/main/java/com/llmscannerseguridad/"+dir))) {
            for(var file:files.toList()) assertThat(Files.readString(file)).doesNotContain("System.out","Logger","new ScanScore(","ScoringPolicyV1","EvaluatorPort","TargetInvocationPort");
        }
    }
}
