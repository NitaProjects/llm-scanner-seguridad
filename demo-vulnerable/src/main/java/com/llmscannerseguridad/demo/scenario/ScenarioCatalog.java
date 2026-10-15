package com.llmscannerseguridad.demo.scenario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScenarioCatalog {
    public static final String VERSION = "1";
    public static final String UNKNOWN = "DEMO_NO_SCENARIO";
    public record Scenario(String code, int revision, String category, String prompt,
            Map<DemoProfile, String> responses) {
        public Scenario {
            responses = Map.copyOf(responses);
            if (code == null || category == null || !code.startsWith(category + "-") || revision != 1
                    || prompt == null || prompt.isBlank() || !responses.keySet().equals(Set.of(DemoProfile.values()))
                    || responses.values().stream().anyMatch(value -> value == null || value.isBlank())) {
                throw new IllegalArgumentException("DEMO_CATALOG_INVALID");
            }
        }
        @Override public String toString() { return "Scenario[" + code + "]"; }
    }

    private final Map<String, String> responses;

    public ScenarioCatalog(DemoProfile profile, List<Scenario> scenarios) {
        var selected = new HashMap<String, String>();
        var categories = new java.util.HashSet<String>();
        for (var scenario : scenarios) {
            if (selected.put(scenario.prompt(), scenario.responses().get(profile)) != null
                    || !categories.add(scenario.category())) throw new IllegalArgumentException("DEMO_CATALOG_INVALID");
        }
        if (!categories.equals(Set.of("LLM01", "LLM02", "LLM08", "LLM10"))) {
            throw new IllegalArgumentException("DEMO_CATALOG_INVALID");
        }
        responses = Map.copyOf(selected);
    }

    public String respond(String message) {
        return responses.getOrDefault(message, UNKNOWN);
    }
}
