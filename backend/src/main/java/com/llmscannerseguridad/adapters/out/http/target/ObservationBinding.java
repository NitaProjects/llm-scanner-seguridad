package com.llmscannerseguridad.adapters.out.http.target;

import static com.llmscannerseguridad.adapters.out.http.target.LocalSessionBindings.object;
import static com.llmscannerseguridad.adapters.out.http.target.LocalSessionBindings.string;
import static com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence.Kind.*;
import static com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence.Source.*;

import java.util.*;
import com.llmscannerseguridad.adapters.out.validation.target.JsonTargetConfigurationValidator;
import com.llmscannerseguridad.adapters.out.validation.target.LocalTargetUrlPolicy;
import com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence.Kind;
import com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence.Source;
import com.llmscannerseguridad.domain.target.TargetRules;

record ObservationBinding(List<Rule> rules, Set<String> retrievalAliases, Set<String> toolAliases,
        Map<String, Map<Source, Boolean>> semanticSourcesByTest) {
    private static final JsonTargetConfigurationValidator PATHS =
            new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of()));

    record Rule(Kind kind, String event, List<Object> items, List<Object> text, List<Object> alias,
            List<Object> id, Set<String> exportValues) { }

    static ObservationBinding parse(Object raw, StreamBinding stream, boolean terminal) {
        var binding = object(raw, Set.of("rules"), Set.of("retrievalAliases", "toolAliases", "semanticSources"));
        if (!(binding.get("rules") instanceof List<?> entries) || entries.isEmpty() || entries.size() > 16) throw invalid();
        var rules = new ArrayList<Rule>();
        var events = new HashSet<String>();
        var kinds = EnumSet.noneOf(Kind.class);
        for (var rawRule : entries) {
            var rule = object(rawRule, Set.of("kind", "textPath"), Set.of("event", "itemsPath", "aliasPath", "callIdPath", "exportValues", "localOnly"));
            Kind kind = Kind.valueOf(string(rule, "kind", 16));
            if (kind == PROMPT || kind == ANSWER || stream == null && !kinds.add(kind)) throw invalid();
            String event = rule.containsKey("event") ? string(rule, "event", 128) : null;
            if (stream == null ? event != null : event == null || !events.add(event) || event.equals(stream.contentEvent())
                    || event.equals(stream.errorEvent()) || event.equals(stream.terminalEvent())
                    || !stream.sse() && stream.eventPath() == null || stream.contentEvent() == null) throw invalid();
            List<Object> alias = path(rule, "aliasPath"), id = path(rule, "callIdPath");
            if (kind == REQUEST && alias == null || kind != REQUEST && kind != ITEM && alias != null
                    || (kind == REQUEST || kind == RESULT) != (id != null)) throw invalid();
            var values = strings(rule, "exportValues", 4096, 32);
            if (rule.containsKey("localOnly") && !(rule.get("localOnly") instanceof Boolean)) throw invalid();
            if (Boolean.TRUE.equals(rule.get("localOnly")) && !values.isEmpty()) throw invalid();
            if (kind == MODEL_TEXT && !values.isEmpty()) throw invalid();
            rules.add(new Rule(kind, event, path(rule, "itemsPath"), path(rule, "textPath"), alias, id, values));
        }
        if (stream == null) rules.sort(Comparator.comparingInt(rule -> rule.kind() == RESULT ? 1 : 0));
        var semantic = new HashMap<String, Map<Source, Boolean>>();
        if (binding.containsKey("semanticSources")) {
            if (!terminal || !(binding.get("semanticSources") instanceof Map<?, ?> sources)) throw invalid();
            if (sources.size() > 10) throw invalid();
            for (var test : sources.entrySet()) {
                if (!(test.getKey() instanceof String code) || !code.matches("LLM[0-9]{2}-[A-Z0-9-]{1,48}")
                        || !(test.getValue() instanceof Map<?, ?> declarations)) throw invalid();
                var selected = new EnumMap<Source, Boolean>(Source.class);
                for (var entry : declarations.entrySet()) {
                    Source source = Source.valueOf((String) entry.getKey());
                    if (source == USER_INPUT || source == FINAL_OUTPUT || !(entry.getValue() instanceof Boolean required)) throw invalid();
                    selected.put(source, required);
                }
                semantic.put(code, Map.copyOf(selected));
            }
        }
        return new ObservationBinding(List.copyOf(rules), aliases(binding, "retrievalAliases"),
                aliases(binding, "toolAliases"), Map.copyOf(semantic));
    }

    Map<Source, Boolean> semanticSources(String testCode) {
        return testCode == null ? Map.of() : semanticSourcesByTest.getOrDefault(testCode, Map.of());
    }

    private static Set<String> aliases(Map<?, ?> binding, String key) {
        var values = strings(binding, key, 128, 32);
        if (values.stream().anyMatch(value -> !validAlias(value))) throw invalid();
        return values;
    }

    static boolean validAlias(String value) { return value.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}"); }

    private static Set<String> strings(Map<?, ?> fields, String key, int bytes, int max) {
        if (!fields.containsKey(key)) return Set.of();
        if (!(fields.get(key) instanceof List<?> list) || list.size() > max) throw invalid();
        var values = new HashSet<String>();
        for (Object item : list) {
            if (!(item instanceof String value) || value.isEmpty() || TargetRules.bytes(value) > bytes
                    || com.llmscannerseguridad.domain.securitytest.SecurityTest.invalidUnicode(value) || !values.add(value)) throw invalid();
        }
        return Set.copyOf(values);
    }

    private static List<Object> path(Map<?, ?> fields, String key) {
        return fields.containsKey(key) ? List.copyOf(PATHS.pathSegments(string(fields, key, 1024))) : null;
    }

    static IllegalArgumentException invalid() { return new IllegalArgumentException("Binding de observaciones no válido."); }
}
