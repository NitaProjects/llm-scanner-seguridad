package com.llmscannerseguridad.demo.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.llmscannerseguridad.demo.http.DemoJson;
import com.llmscannerseguridad.demo.scenario.DemoProfile;
import com.llmscannerseguridad.demo.scenario.ScenarioCatalog;
import com.llmscannerseguridad.demo.scenario.ScenarioCatalog.Scenario;

public final class CatalogLoader {
    private CatalogLoader() { }

    public static List<Scenario> load() {
        try (var input = CatalogLoader.class.getResourceAsStream("/benchmark/scenarios-v1.json")) {
            if (input == null) throw new IllegalArgumentException();
            return decode(DemoJson.read(input, 16384));
        } catch (Exception error) {
            throw new IllegalArgumentException("DEMO_CATALOG_INVALID");
        }
    }

    public static List<Scenario> decode(byte[] bytes) {
        try {
            var root = object(DemoJson.parse(bytes), Set.of("scenarioVersion", "scenarios"));
            if (!ScenarioCatalog.VERSION.equals(root.get("scenarioVersion"))
                    || !(root.get("scenarios") instanceof List<?> entries) || entries.size() != 4) {
                throw new IllegalArgumentException();
            }
            var scenarios = new ArrayList<Scenario>();
            var codes = new java.util.HashSet<String>();
            for (Object value : entries) {
                var entry = object(value, Set.of("code", "revision", "category", "prompt", "responses"));
                var responses = object(entry.get("responses"), Set.of("SAFE", "VULNERABLE"));
                if (!Integer.valueOf(1).equals(entry.get("revision"))) throw new IllegalArgumentException();
                var scenario = new Scenario((String) entry.get("code"), 1, (String) entry.get("category"),
                        (String) entry.get("prompt"), Map.of(DemoProfile.SAFE, (String) responses.get("SAFE"),
                                DemoProfile.VULNERABLE, (String) responses.get("VULNERABLE")));
                if (scenario.prompt().getBytes(StandardCharsets.UTF_8).length > DemoJson.MESSAGE_BYTES) throw new IllegalArgumentException();
                if (!codes.add(scenario.code())) throw new IllegalArgumentException();
                for (String response : scenario.responses().values()) DemoJson.response(Map.of("response", response));
                scenarios.add(scenario);
            }
            if (!codes.equals(Set.of("LLM01-INSTRUCTION-001", "LLM02-CANARY-001", "LLM08-CONTEXT-001", "LLM10-MARKUP-001"))) {
                throw new IllegalArgumentException();
            }
            new ScenarioCatalog(DemoProfile.SAFE, scenarios);
            new ScenarioCatalog(DemoProfile.VULNERABLE, scenarios);
            DemoJson.response(Map.of("response", ScenarioCatalog.UNKNOWN));
            return List.copyOf(scenarios);
        } catch (Exception error) {
            throw new IllegalArgumentException("DEMO_CATALOG_INVALID");
        }
    }

    private static Map<?, ?> object(Object value, Set<String> fields) {
        if (!(value instanceof Map<?, ?> map) || !map.keySet().equals(fields)) throw new IllegalArgumentException();
        return map;
    }
}
