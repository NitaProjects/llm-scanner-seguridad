package com.llmscannerseguridad.adapters.out.evaluation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import com.llmscannerseguridad.application.evaluation.EvaluationScenarioPort;
import com.llmscannerseguridad.application.evaluation.EvaluationPlan;
import com.llmscannerseguridad.domain.evaluation.SyntheticContext;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;

public final class LocalEvaluationScenarioAdapter implements EvaluationScenarioPort {
    public record ExportPolicy(boolean enabled, String provider) { }
    public record Manifest(int schemaVersion, List<ManifestScenario> scenarios) { }
    public record ManifestScenario(String scenarioId, String revision, UUID projectId, UUID targetConfigurationId,
            String targetUpdatedAt, List<Entry> entries, ExportPolicy exportPolicy) { }
    private final Path file;
    private final boolean enabled;
    private final String provider;
    private final Set<String> allowedScenarios;
    public LocalEvaluationScenarioAdapter(Path file, boolean enabled, String provider, Set<String> allowedScenarios) {
        if (file != null && (file.toString().startsWith("\\\\") || !file.getFileName().toString().endsWith(".json"))) throw EvaluationJson.invalid();
        this.file = file; this.enabled = enabled; this.provider = provider; this.allowedScenarios = Set.copyOf(allowedScenarios);
    }
    @Override public Scenario snapshot(UUID projectId, UUID targetId, Instant targetUpdatedAt, List<EvaluationPlan.Item> tests) {
        if (file == null) return null;
        try (var stream = Files.newInputStream(file)) {
            String text = EvaluationJson.read(stream, 262144);
            Object parsed = EvaluationJson.parse(text);
            if (parsed instanceof Map<?, ?> root && java.math.BigInteger.TWO.equals(root.get("schemaVersion"))) {
                return snapshotV2(root, projectId, targetId, targetUpdatedAt, tests);
            }
            var manifest = EvaluationJson.MAPPER.readValue(text, Manifest.class);
            if (manifest.schemaVersion() != 1 || manifest.scenarios() == null || manifest.scenarios().size() > 16) throw EvaluationJson.invalid();
            var ids = new HashSet<String>();
            ManifestScenario selected = null;
            for (var scenario : manifest.scenarios()) {
                validate(scenario, tests);
                if (!ids.add(scenario.scenarioId())) throw EvaluationJson.invalid();
                if (projectId.equals(scenario.projectId()) && targetId.equals(scenario.targetConfigurationId())
                        && targetUpdatedAt.equals(Instant.parse(scenario.targetUpdatedAt()))) {
                    if (selected != null) throw EvaluationJson.invalid();
                    selected = scenario;
                }
            }
            if (selected == null) return null;
            Map<String, Entry> entries = new HashMap<>();
            selected.entries().forEach(entry -> entries.put(entry.securityTestCode(), entry));
            return new Scenario(selected.scenarioId(), selected.revision(), EvaluationJson.hash(EvaluationJson.canonical(manifest)),
                    enabled && allowedScenarios.contains(selected.scenarioId()) && selected.exportPolicy().enabled()
                        && provider.equalsIgnoreCase(selected.exportPolicy().provider()), provider, entries);
        } catch (Exception error) { throw EvaluationJson.invalid(); }
    }

    private Scenario snapshotV2(Map<?, ?> root, UUID projectId, UUID targetId, Instant updatedAt,
            List<EvaluationPlan.Item> tests) {
        fields(root, Set.of("schemaVersion", "scenarios"), Set.of());
        if (!(root.get("scenarios") instanceof List<?> scenarios) || scenarios.size() > 16) throw EvaluationJson.invalid();
        var ids = new HashSet<String>();
        Scenario selected = null;
        for (var value : scenarios) {
            var scenario = fields(value, Set.of("scenarioId", "revision", "projectId", "targetConfigurationId", "targetUpdatedAt", "entries"), Set.of("exportPolicy"));
            String id = EvaluationJson.string(scenario.get("scenarioId"));
            String revision = EvaluationJson.string(scenario.get("revision"));
            if (!id.matches("[A-Za-z0-9-]{1,64}") || !ids.add(id) || !revision.matches("[A-Za-z0-9.-]{1,32}")) throw EvaluationJson.invalid();
            var binding = new Binding(uuid(scenario.get("projectId")), uuid(scenario.get("targetConfigurationId")),
                    Instant.parse(EvaluationJson.string(scenario.get("targetUpdatedAt"))));
            if (binding.targetUpdatedAt().getNano() % 1000 != 0) throw EvaluationJson.invalid();
            boolean export = false;
            if (scenario.containsKey("exportPolicy")) {
                var policy = fields(scenario.get("exportPolicy"), Set.of("enabled", "provider"), Set.of());
                if (!(policy.get("enabled") instanceof Boolean active) || !(policy.get("provider") instanceof String name)
                        || !"OPENAI".equalsIgnoreCase(name)) throw EvaluationJson.invalid();
                export = active && enabled && allowedScenarios.contains(id) && name.equalsIgnoreCase(provider);
            }
            if (!(scenario.get("entries") instanceof List<?> values) || values.isEmpty() || values.size() > 4) throw EvaluationJson.invalid();
            var entries = new HashMap<String, Entry>();
            int contextBytes = 0;
            for (var item : values) {
                var entry = fields(item, Set.of("securityTestCode", "securityTestRevision", "context"), Set.of("exportableResponses"));
                String code = EvaluationJson.string(entry.get("securityTestCode"));
                int testRevision = EvaluationJson.integer(entry.get("securityTestRevision"));
                var test = tests.stream().filter(t -> t.securityTestCode().equals(code) && t.securityTestRevision() == testRevision)
                        .findFirst().orElseThrow(EvaluationJson::invalid);
                var context = contextV2(entry.get("context"), test);
                contextBytes += EvaluationJson.bytes(EvaluationJson.write(context));
                var responses = entry.containsKey("exportableResponses") ? strings(entry.get("exportableResponses")) : List.<String>of();
                if (contextBytes > 32768 || responses.size() > 64 || entries.putIfAbsent(code, new Entry(code, testRevision, context, responses)) != null) {
                    throw EvaluationJson.invalid();
                }
            }
            if (binding.equals(new Binding(projectId, targetId, updatedAt))) {
                if (selected != null) throw EvaluationJson.invalid();
                selected = new Scenario(id, revision, EvaluationJson.hash(EvaluationJson.canonical(root)), export, provider, entries, binding);
            }
        }
        return selected;
    }

    private SyntheticContext contextV2(Object value, EvaluationPlan.Item test) {
        var required = switch (test.category()) {
            case LLM01 -> "restriction";
            case LLM02 -> "reservedValue";
            case LLM08 -> "hiddenContext";
            case LLM10 -> "outputContract";
        };
        var optional = new HashSet<>(Set.of("publicValues", "expectedSafeResponses"));
        if (test.category().name().equals("LLM01")) optional.addAll(Set.of("actionMarker", "markerViolationConfirmed"));
        if (test.category().name().equals("LLM08")) optional.add("hiddenAnchors");
        var fields = fields(value, Set.of(required), optional);
        var publicValues = optionalStrings(fields, "publicValues");
        var safe = optionalStrings(fields, "expectedSafeResponses");
        return switch (test.category()) {
            case LLM01 -> {
                String marker = fields.containsKey("actionMarker") ? literal(fields.get("actionMarker")) : null;
                Object confirmed = fields.containsKey("markerViolationConfirmed") ? fields.get("markerViolationConfirmed") : false;
                if (!(confirmed instanceof Boolean flag) || flag && marker == null) throw EvaluationJson.invalid();
                yield new SyntheticContext(null, null, List.of(), publicValues, literal(fields.get(required)), marker, flag, safe, null, false, null, null);
            }
            case LLM02 -> new SyntheticContext(literal(fields.get(required)), null, List.of(), publicValues, null, null, false, safe, null, false, null, null);
            case LLM08 -> new SyntheticContext(null, literal(fields.get(required)), optionalStrings(fields, "hiddenAnchors"), publicValues, null, null, false, safe, null, false, null, null);
            case LLM10 -> new SyntheticContext(null, null, List.of(), publicValues, null, null, false, safe, contractV2(fields.get(required)), false, null, null);
        };
    }

    private SyntheticContext.OutputContract contractV2(Object value) {
        var contract = fields(value, Set.of("securityRelevant"), Set.of("allowedKeys", "requiredKeys", "propertyTypes", "forbiddenLiterals"));
        if (!Boolean.TRUE.equals(contract.get("securityRelevant")) || !contract.containsKey("allowedKeys")) throw EvaluationJson.invalid();
        var allowed = strings(contract.get("allowedKeys"));
        var required = optionalStrings(contract, "requiredKeys");
        var types = new HashMap<String, SyntheticContext.ValueType>();
        if (contract.containsKey("propertyTypes")) {
            if (!(contract.get("propertyTypes") instanceof Map<?, ?> properties)) throw EvaluationJson.invalid();
            properties.forEach((key, type) -> types.put(literal(key), SyntheticContext.ValueType.valueOf(EvaluationJson.string(type))));
        }
        return new SyntheticContext.OutputContract(optionalStrings(contract, "forbiddenLiterals"), required, allowed, types, true);
    }

    private static Map<?, ?> fields(Object value, Set<String> required, Set<String> optional) {
        if (!(value instanceof Map<?, ?> map) || !map.keySet().containsAll(required)
                || map.keySet().stream().anyMatch(key -> !required.contains(key) && !optional.contains(key))) throw EvaluationJson.invalid();
        return map;
    }

    private static UUID uuid(Object value) {
        String text = EvaluationJson.string(value);
        var id = UUID.fromString(text);
        if (!id.toString().equalsIgnoreCase(text)) throw EvaluationJson.invalid();
        return id;
    }

    private static String literal(Object value) {
        String text = EvaluationJson.string(value);
        if (text.isBlank() || text.indexOf('\0') >= 0) throw EvaluationJson.invalid();
        return text;
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) throw EvaluationJson.invalid();
        return list.stream().map(LocalEvaluationScenarioAdapter::literal).toList();
    }

    private static List<String> optionalStrings(Map<?, ?> fields, String key) {
        return fields.containsKey(key) ? strings(fields.get(key)) : List.of();
    }
    private void validate(ManifestScenario scenario, List<EvaluationPlan.Item> tests) {
        if (scenario.scenarioId() == null || !scenario.scenarioId().matches("[A-Za-z0-9-]{1,64}")
                || scenario.revision() == null || !scenario.revision().matches("[A-Za-z0-9.-]{1,32}")
                || scenario.projectId() == null || scenario.targetConfigurationId() == null
                || scenario.entries() == null || scenario.entries().size() != 4 || scenario.exportPolicy() == null
                || !"OPENAI".equalsIgnoreCase(scenario.exportPolicy().provider())) throw EvaluationJson.invalid();
        Instant updated = Instant.parse(scenario.targetUpdatedAt());
        if (updated.getNano() % 1000 != 0) throw EvaluationJson.invalid();
        var codes = new HashSet<String>();
        int contextBytes = 0;
        for (var entry : scenario.entries()) {
            if (!codes.add(entry.securityTestCode()) || entry.context() == null || entry.exportableResponses().size() > 64
                    || tests.stream().noneMatch(test -> test.securityTestCode().equals(entry.securityTestCode())
                        && test.securityTestRevision() == entry.securityTestRevision())) throw EvaluationJson.invalid();
            contextBytes += EvaluationJson.bytes(EvaluationJson.write(entry.context()));
            if (contextBytes > 32768) throw EvaluationJson.invalid();
            validateAnchors(entry.context());
        }
    }
    private void validateAnchors(SyntheticContext context) {
        if (context.hiddenAnchors().isEmpty()) return;
        if (context.hiddenContext() == null || context.hiddenAnchors().get(0).equals(context.hiddenAnchors().get(1))) throw EvaluationJson.invalid();
        var clauses = context.hiddenContext().split("[.;\\n]");
        Set<Integer> positions = new HashSet<>();
        for (var anchor : context.hiddenAnchors()) {
            if (anchor.codePointCount(0, anchor.length()) < 12 || anchor.codePointCount(0, anchor.length()) > 240) throw EvaluationJson.invalid();
            int found = -1;
            for (int i = 0; i < clauses.length; i++) if (clauses[i].contains(anchor)) { if (found != -1) throw EvaluationJson.invalid(); found = i; }
            if (found < 0 || !positions.add(found)) throw EvaluationJson.invalid();
        }
    }
}
