package com.llmscannerseguridad.demo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.llmscannerseguridad.demo.config.CatalogLoader;
import com.llmscannerseguridad.demo.scenario.DemoProfile;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ManifestTest {
    @Test void closedLocalSchemasRejectUnknownMissingAndWrongTypes() throws Exception {
        for (var pair : Map.of("manifest/ground-truth-v1.json", "ground-truth-v1",
                "manifest/benchmark-v1.json", "benchmark-v1",
                "src/main/resources/benchmark/scenarios-v1.json", "scenarios-v1").entrySet()) {
            var value = TestJson.object(TestJson.read(pair.getKey()));
            Object schema = TestJson.read("manifest/schemas/" + pair.getValue() + ".schema.json");
            JsonSchemaCheck.validate(value, schema);
            var unknown = new java.util.LinkedHashMap<>(value);
            unknown.put("ORACLE_ONLY_UNKNOWN_FIELD", true);
            assertThatThrownBy(() -> JsonSchemaCheck.validate(unknown, schema)).isInstanceOf(AssertionError.class);
            var missing = new java.util.LinkedHashMap<>(value);
            missing.remove(value.keySet().iterator().next());
            assertThatThrownBy(() -> JsonSchemaCheck.validate(missing, schema)).isInstanceOf(AssertionError.class);
        }
    }

    @Test void eightUniqueCasesAndSignalsMatchTheDeclaredRestrictions() throws Exception {
        var root = TestJson.object(TestJson.read("manifest/ground-truth-v1.json"));
        var ids = new HashSet<String>();
        var pairs = new HashSet<String>();
        var rules = Map.of("LLM01", "EXPECTED_MARKER", "LLM02", "KNOWN_SYNTHETIC_VALUE",
                "LLM08", "KNOWN_HIDDEN_CONTEXT", "LLM10", "OUTPUT_CONTRACT");
        for (Object item : (List<?>) root.get("cases")) {
            var record = TestJson.object(item);
            assertThat(ids.add((String) record.get("caseId"))).isTrue();
            assertThat(pairs.add(record.get("category") + "/" + record.get("profile"))).isTrue();
            boolean vulnerable = record.get("profile").equals("VULNERABLE");
            assertThat(record.get("vulnerable")).isEqualTo(vulnerable);
            assertThat(record.get("expectedVerdict")).isEqualTo(vulnerable ? "VULNERABLE" : "NOT_VULNERABLE");
            var signal = TestJson.object(record.get("expectedSignal"));
            assertThat(signal.get("ruleId")).isEqualTo(vulnerable ? rules.get(record.get("category")) : "EXPECTED_SAFE_RESPONSE");
            assertThat(signal.get("strength")).isEqualTo(vulnerable ? "STRONG_POSITIVE" : "STRONG_NEGATIVE");
            assertThat(record.get("coverage")).isEqualTo(record.get("category").equals("LLM10") ? "OUTPUT_ONLY" : "SYNTHETIC_BEHAVIOR");
        }
        assertThat(ids).hasSize(8);
    }

    @Test void evaluatorTemplatesKeepOnlyTechnicalContextAndOneExactCorpusPerProfile() throws Exception {
        var schema = TestJson.read("manifest/schemas/evaluation-scenario-v1.schema.json");
        var contexts = new java.util.ArrayList<Object>();
        var catalog = CatalogLoader.load();
        for (var profile : DemoProfile.values()) {
            var template = TestJson.object(TestJson.read("manifest/evaluation-" + profile.name().toLowerCase() + ".template.json"));
            // Los tokens no son UUID/Instant: una plantilla no es un binding utilizable.
            assertThatThrownBy(() -> JsonSchemaCheck.validate(template, schema)).isInstanceOf(AssertionError.class);
            var scenario = TestJson.object(((List<?>) template.get("scenarios")).getFirst());
            assertThat(scenario.get("projectId")).isEqualTo("REPLACE_PROJECT_ID");
            assertThat(scenario.get("targetConfigurationId")).isEqualTo("REPLACE_TARGET_ID");
            assertThat(scenario.get("targetUpdatedAt")).isEqualTo("REPLACE_TARGET_UPDATED_AT");
            scenario.put("projectId", UUID.randomUUID().toString());
            scenario.put("targetConfigurationId", UUID.randomUUID().toString());
            scenario.put("targetUpdatedAt", "2026-10-15T12:34:56.123456Z"); // Binding exclusivo de test.
            JsonSchemaCheck.validate(template, schema);
            rejectOracleKeys(template);
            assertThat(TestJson.object(scenario.get("exportPolicy"))).containsEntry("enabled", false).containsEntry("provider", "OPENAI");
            var entries = (List<?>) scenario.get("entries");
            var codes = new HashSet<String>();
            int contextBytes = 0;
            for (int i = 0; i < entries.size(); i++) {
                var entry = TestJson.object(entries.get(i));
                assertThat(codes.add((String) entry.get("securityTestCode"))).isTrue();
                assertThat(entry.get("securityTestCode")).isEqualTo(catalog.get(i).code());
                assertThat(entry.get("exportableResponses")).isEqualTo(List.of(catalog.get(i).responses().get(profile)));
                var context = TestJson.object(entry.get("context"));
                contextBytes += TestJson.MAPPER.writeValueAsBytes(context).length;
                if (profile == DemoProfile.SAFE) contexts.add(context);
                else assertThat(context).isEqualTo(contexts.get(i));
                if (i < 3) {
                    String reserved = (String) context.get(List.of("actionMarker", "reservedValue", "hiddenContext").get(i));
                    assertThat(reserved).isNotBlank();
                    assertThat(catalog.get(i).prompt()).doesNotContain(reserved);
                    for (Object value : (List<?>) context.get("publicValues")) assertThat((String) value).doesNotContain(reserved);
                }
                if (i == 0) assertThat(context).containsEntry("markerViolationConfirmed", true);
                if (i == 2) {
                    var anchors = (List<?>) context.get("hiddenAnchors");
                    assertThat(anchors).hasSize(2);
                    String[] clauses = ((String) context.get("hiddenContext")).split("[.;\\n]");
                    for (int a = 0; a < 2; a++) {
                        assertThat((String) anchors.get(a)).hasSizeGreaterThanOrEqualTo(12);
                        assertThat(clauses[a]).contains((String) anchors.get(a));
                        assertThat(catalog.get(i).prompt()).doesNotContain((String) anchors.get(a));
                    }
                }
            }
            assertThat(contextBytes).isLessThanOrEqualTo(32768);
            assertThat(TestJson.MAPPER.writeValueAsBytes(template).length).isLessThanOrEqualTo(262144);
            var both = new java.util.LinkedHashMap<>(template);
            both.put("scenarios", List.of(scenario, scenario));
            assertThatThrownBy(() -> JsonSchemaCheck.validate(both, schema)).isInstanceOf(AssertionError.class);
            scenario.put("targetUpdatedAt", "2026-10-15T12:34:56.1234567Z");
            assertThatThrownBy(() -> JsonSchemaCheck.validate(template, schema)).isInstanceOf(AssertionError.class);
        }
    }

    @Test void exportPolicyRequiresExplicitBooleanAndTheExistingProviderWithoutChangingOfflineTemplates() throws Exception {
        var schema = TestJson.read("manifest/schemas/evaluation-scenario-v1.schema.json");
        for (var profile : DemoProfile.values()) {
            var template = TestJson.object(TestJson.read("manifest/evaluation-" + profile.name().toLowerCase() + ".template.json"));
            assertThat(template.get("schemaVersion")).isEqualTo(1);
            var scenario = TestJson.object(((List<?>) template.get("scenarios")).getFirst());
            var policy = TestJson.object(scenario.get("exportPolicy"));
            assertThat(policy).containsEntry("enabled", false).containsEntry("provider", "OPENAI");
            scenario.put("projectId", UUID.randomUUID().toString());
            scenario.put("targetConfigurationId", UUID.randomUUID().toString());
            scenario.put("targetUpdatedAt", "2026-10-15T12:34:56.123456Z");
            for (boolean enabled : List.of(false, true)) {
                policy.put("enabled", enabled);
                JsonSchemaCheck.validate(template, schema);
            }
            policy.remove("enabled");
            assertThatThrownBy(() -> JsonSchemaCheck.validate(template, schema)).isInstanceOf(AssertionError.class);
            assertThat(policy).doesNotContainKey("enabled");
            for (Object invalid : java.util.Arrays.asList(null, "true", "false", 0, 1, List.of(), Map.of())) {
                policy.put("enabled", invalid);
                assertThatThrownBy(() -> JsonSchemaCheck.validate(template, schema)).isInstanceOf(AssertionError.class);
                assertThat(policy.get("enabled")).isEqualTo(invalid);
            }
            policy.put("enabled", true);
            policy.put("provider", "OTHER");
            assertThatThrownBy(() -> JsonSchemaCheck.validate(template, schema)).isInstanceOf(AssertionError.class);
            policy.remove("provider");
            assertThatThrownBy(() -> JsonSchemaCheck.validate(template, schema)).isInstanceOf(AssertionError.class);
        }
    }

    @Test void documentedTargetMatchesPublicScannerContractAndV4Probes() throws Exception {
        var benchmark = TestJson.object(TestJson.read("manifest/benchmark-v1.json"));
        var target = TestJson.object(benchmark.get("targetConfiguration"));
        assertThat(target).containsEntry("url", "http://127.0.0.1:8081/api/chat")
                .containsEntry("method", "POST").containsEntry("contentType", "application/json")
                .containsEntry("responsePath", "$.response").containsEntry("authType", "NONE").containsEntry("timeoutMs", 5000);
        assertThat(target).doesNotContainKey("authConfig");
        assertThat(TestJson.MAPPER.readValue((String) target.get("requestTemplate"), Object.class)).isEqualTo(Map.of("message", "{{PROMPT}}"));
        var reference = TestJson.object(TestJson.read("src/test/resources/benchmark/probes-v4-reference.json"));
        var probes = (List<?>) reference.get("probes");
        assertThat(probes).hasSize(4);
        var scenarios = CatalogLoader.load();
        for (int i = 0; i < probes.size(); i++) {
            var expected = TestJson.object(probes.get(i));
            assertThat(scenarios.get(i).code()).isEqualTo(expected.get("code"));
            assertThat(scenarios.get(i).revision()).isEqualTo(expected.get("revision"));
            assertThat(scenarios.get(i).prompt()).isEqualTo(expected.get("prompt"));
        }
    }

    @Test void localBindingRequiresHealthProfileVersionIdentityAndExactTimestamp() throws Exception {
        var binding = new java.util.LinkedHashMap<String, Object>();
        binding.put("schemaVersion", 1); binding.put("benchmarkVersion", "1"); binding.put("profile", "SAFE");
        binding.put("scenarioId", "demo-safe-v1"); binding.put("projectId", UUID.randomUUID().toString());
        binding.put("targetConfigurationId", UUID.randomUUID().toString());
        binding.put("targetUpdatedAt", "2026-10-15T12:34:56.123456Z");
        binding.put("checkedHealth", Map.of("status", "UP", "demo", true, "profile", "SAFE", "scenarioVersion", "1"));
        binding.put("authorizationDeclaredByOperator", true);
        var schema = TestJson.read("manifest/schemas/bindings-v1.schema.json");
        JsonSchemaCheck.validate(binding, schema);
        binding.put("checkedHealth", Map.of("status", "UP", "demo", true, "profile", "VULNERABLE", "scenarioVersion", "1"));
        assertThatThrownBy(() -> JsonSchemaCheck.validate(binding, schema)).isInstanceOf(AssertionError.class);
        binding.put("checkedHealth", Map.of("status", "UP", "demo", true, "profile", "SAFE", "scenarioVersion", "1"));
        binding.put("scenarioId", "demo-vulnerable-v1");
        assertThatThrownBy(() -> JsonSchemaCheck.validate(binding, schema)).isInstanceOf(AssertionError.class);
        binding.put("scenarioId", "demo-safe-v1");
        binding.put("authorizationDeclaredByOperator", false);
        assertThatThrownBy(() -> JsonSchemaCheck.validate(binding, schema)).isInstanceOf(AssertionError.class);
        String ignore = Files.readString(Path.of(".gitignore"));
        assertThat(ignore).contains("/manifest/bindings.local.json", "/manifest/evaluation-scenario.local.json");
    }

    private void rejectOracleKeys(Object value) {
        var prohibited = Set.of("caseId", "profile", "vulnerable", "expectedVerdict", "expectedSignal", "expectedResponse",
                "justification", "rationale", "benchmarkClassification", "expectedScannerResult", "observedResult",
                "TP", "TN", "FP", "FN", "url", "headers", "groundTruth");
        if (value instanceof Map<?, ?> map) {
            assertThat(map.keySet().stream().noneMatch(prohibited::contains)).isTrue();
            map.values().forEach(this::rejectOracleKeys);
        } else if (value instanceof List<?> list) list.forEach(this::rejectOracleKeys);
    }
}
