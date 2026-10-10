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
            EvaluationJson.parse(text);
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
