package com.llmscannerseguridad.application.evaluation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.llmscannerseguridad.domain.evaluation.SyntheticContext;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;

public interface EvaluationScenarioPort {
    Scenario snapshot(UUID projectId, UUID targetConfigurationId, Instant targetUpdatedAt, List<EvaluationPlan.Item> tests);
    record Entry(String securityTestCode, int securityTestRevision, SyntheticContext context,
            List<String> exportableResponses) {
        public Entry { exportableResponses = List.copyOf(exportableResponses); }
        @Override public String toString() { return "ScenarioEntry[protegida]"; }
    }
    record Scenario(String scenarioId, String revision, String manifestHash, boolean exportAuthorized,
            String exportProvider, Map<String, Entry> entries, Binding binding) {
        public Scenario(String scenarioId, String revision, String manifestHash, boolean exportAuthorized,
                String exportProvider, Map<String, Entry> entries) {
            this(scenarioId, revision, manifestHash, exportAuthorized, exportProvider, entries, null);
        }
        public Scenario { entries = Map.copyOf(entries); }
        public boolean v2() { return binding != null; }
        @Override public String toString() { return "EvaluationScenario[protegido]"; }
    }
    record Binding(UUID projectId, UUID targetConfigurationId, Instant targetUpdatedAt) { }
}
