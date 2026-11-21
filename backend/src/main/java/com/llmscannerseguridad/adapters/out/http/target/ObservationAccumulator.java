package com.llmscannerseguridad.adapters.out.http.target;

import static com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence.Availability.*;
import static com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence.Kind.*;
import static com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence.Source.*;

import java.util.*;
import java.util.function.UnaryOperator;
import com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence;
import com.llmscannerseguridad.application.evaluation.StructuredInteractionEvidence.*;
import com.llmscannerseguridad.domain.target.TargetRules;
import tools.jackson.databind.json.JsonMapper;

/** Acumula exclusivamente campos seleccionados de una interacción autorizada. */
final class ObservationAccumulator {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ObservationAccumulator.class);
    private static final Object ABSENT = new Object();
    private final int textBytes;
    private final List<Observation> observations = new ArrayList<>();
    private final Map<String, Integer> calls = new HashMap<>();
    private final Set<String> ambiguousCalls = new HashSet<>();
    private final Set<String> results = new HashSet<>();
    private final Set<String> remoteIds = new HashSet<>();
    private final EnumMap<Kind, Integer> rejectedAliases = new EnumMap<>(Kind.class);
    private final EnumMap<Kind, Integer> counts = new EnumMap<>(Kind.class);
    private final EnumMap<Source, Availability> availability = new EnumMap<>(Source.class);
    private final Map<Integer, List<String>> originals = new HashMap<>();
    private int bytes = 2;
    private boolean inspectionIncomplete;

    ObservationAccumulator(int textBytes) { this.textBytes = textBytes; }

    void accept(ObservationBinding binding, String event, Object document, UnaryOperator<String> redact, Runnable deadline) {
        for (var rule : binding.rules()) {
            if (!Objects.equals(rule.event(), event)) continue;
            var source = source(rule.kind());
            if (availability.get(source) == TOO_LARGE) continue;
            if (rule.items() == null) {
                append(binding, rule, document, redact);
            } else if (select(document, rule.items()) instanceof List<?> items) {
                if (items.size() > maximum(rule.kind()) - counts.getOrDefault(rule.kind(), 0)) {
                    invalidate(source, TOO_LARGE);
                    continue;
                }
                for (Object item : items) {
                    deadline.run();
                    append(binding, rule, item, redact);
                    if (availability.get(source) == TOO_LARGE) break;
                }
            } else if (select(document, rule.items()) == ABSENT && document != null) {
                availability.putIfAbsent(source, INCOMPLETE);
            } else {
                invalidate(source, INCOMPLETE);
            }
        }
    }

    private void append(ObservationBinding binding, ObservationBinding.Rule rule, Object item, UnaryOperator<String> redact) {
        var kind = rule.kind();
        var source = source(kind);
        if (counts.merge(kind, 1, Integer::sum) > maximum(kind)) {
            invalidate(source, TOO_LARGE);
            return;
        }
        Object raw = select(item, rule.text());
        Object rawAlias = rule.alias() == null ? null : select(item, rule.alias());
        if (raw == ABSENT && item != null && kind == QUERY) {
            incomplete(source);
            return;
        }
        if (!(raw instanceof String text) || rule.alias() != null && !(rawAlias instanceof String)) {
            invalidate(source, INCOMPLETE);
            return;
        }
        if (TargetRules.bytes(text) > 4096 || rawAlias instanceof String name && TargetRules.bytes(name) > 4096) {
            invalidate(source, TOO_LARGE);
            return;
        }
        if (!rule.exportValues().contains(text)) {
            com.llmscannerseguridad.privacy.ProtectionScope.register(text);
            if (rawAlias instanceof String name) com.llmscannerseguridad.privacy.ProtectionScope.register(name);
        }
        String safe = redact.apply(text);
        String alias = rawAlias instanceof String name ? redact.apply(name) : null;
        boolean aliasValid = alias == null || ObservationBinding.validAlias(alias);
        boolean listed = alias == null || (kind == ITEM ? binding.retrievalAliases() : binding.toolAliases()).contains(alias);
        if (!listed || !aliasValid) rejectedAliases.merge(kind, 1, Integer::sum);
        if (!aliasValid || !safe.equals(text) || !Objects.equals(alias, rawAlias)) inspectionIncomplete = true;
        int ordinal = counts.values().stream().mapToInt(Integer::intValue).sum();
        Integer related = null;
        boolean correlated = true;
        if (rule.id() != null) {
            Object id = select(item, rule.id());
            if (id instanceof String || id instanceof Boolean || id instanceof java.math.BigInteger || id instanceof java.math.BigDecimal) {
                com.llmscannerseguridad.privacy.ProtectionScope.register(id.toString());
                String key = (id instanceof String ? "s:" : "v:") + id;
                if (TargetRules.bytes(key.substring(2)) > 128) {
                    invalidate(source, TOO_LARGE);
                    correlated = false;
                } else if (id.toString().isBlank()) {
                    correlated = false;
                } else if (kind == REQUEST) {
                    rememberId(id.toString(), redact);
                    correlated = calls.putIfAbsent(key, ordinal) == null;
                    if (!correlated) ambiguousCalls.add(key);
                } else {
                    rememberId(id.toString(), redact);
                    related = calls.get(key);
                    correlated = related != null && !ambiguousCalls.contains(key) && results.add(key);
                    if (!correlated && related != null) ambiguousCalls.add(key);
                    if (!correlated) related = null;
                }
            } else {
                correlated = false;
                if (id != ABSENT && id != null) invalidate(source, INCOMPLETE);
            }
        }
        if (!correlated || !aliasValid) {
            incomplete(source);
            if (kind == REQUEST || kind == RESULT) incomplete(source == TOOL_ARGUMENT ? TOOL_RESULT : TOOL_ARGUMENT);
        }
        boolean eligible = aliasValid && listed && correlated && safe.equals(text) && Objects.equals(alias, rawAlias)
                && rule.exportValues().contains(safe);
        var observation = new Observation(source, kind, ordinal, related, alias, safe, eligible, correlated && aliasValid);
        int size = JsonMapper.shared().writeValueAsBytes(observation).length;
        if (size > 4096 || bytes + size + (observations.isEmpty() ? 0 : 1) > textBytes) {
            invalidate(source, TOO_LARGE);
            return;
        }
        bytes += size + (observations.isEmpty() ? 0 : 1);
        availability.putIfAbsent(source, COMPLETE);
        originals.put(ordinal, rawAlias instanceof String name ? List.of(text, name) : List.of(text));
        observations.add(observation);
    }

    StructuredInteractionEvidence finish(UUID executionId, Map<Source, Boolean> semanticSources, UnaryOperator<String> redact) {
        if (calls.keySet().stream().anyMatch(id -> !results.contains(id))) {
            availability.putIfAbsent(TOOL_RESULT, INCOMPLETE);
            availability.replace(TOOL_RESULT, COMPLETE, INCOMPLETE);
            availability.replace(TOOL_ARGUMENT, COMPLETE, INCOMPLETE);
        }
        var sanitized = new ArrayList<Observation>();
        var ambiguousOrdinals = new HashSet<Integer>();
        ambiguousCalls.forEach(key -> ambiguousOrdinals.add(calls.get(key)));
        int total = 2;
        for (var item : observations) {
            if (!item.exportable() || !item.complete() || availability.get(item.source()) != COMPLETE
                    || !semanticSources.containsKey(item.source())) {
                originals.getOrDefault(item.ordinal(), List.of()).forEach(com.llmscannerseguridad.privacy.ProtectionScope::register);
            }
            String value = redact.apply(item.value());
            String alias = item.alias() == null ? null : redact.apply(item.alias());
            boolean intact = value.equals(item.value()) && Objects.equals(alias, item.alias());
            if (!intact) inspectionIncomplete = true;
            Integer relation = item.relatedOrdinal();
            if (relation != null && ambiguousOrdinals.contains(relation)) relation = null;
            var safe = new Observation(item.source(), item.kind(), item.ordinal(), relation, alias, value,
                    item.exportable() && intact, item.complete() && intact && availability.get(item.source()) == COMPLETE);
            int size = JsonMapper.shared().writeValueAsBytes(safe).length;
            int separator = sanitized.isEmpty() ? 0 : 1;
            if (size > 4096 || total + size + separator > textBytes) {
                invalidate(item.source(), TOO_LARGE);
            } else {
                total += size + separator;
                sanitized.add(safe);
            }
        }
        LOG.debug("testExecution={} observationCounts={} observationAvailability={} rejectedAliases={}",
                executionId, counts, availability, rejectedAliases);
        var safeIds = new HashSet<String>();
        for (String id : remoteIds) {
            String safe = redact.apply(id);
            if (!safe.equals(id)) inspectionIncomplete = true;
            if (TargetRules.bytes(safe) <= 128) safeIds.add(safe);
            else inspectionIncomplete = true;
        }
        com.llmscannerseguridad.privacy.ProtectionScope.coverObservations(executionId);
        return new StructuredInteractionEvidence(executionId, sanitized, availability, semanticSources, inspectionIncomplete, safeIds);
    }

    private void invalidate(Source source, Availability reason) {
        if (reason == TOO_LARGE || availability.get(source) != TOO_LARGE) availability.put(source, reason);
        // Sin el campo original no se puede comprobar si reaparece en la respuesta final.
        inspectionIncomplete = true;
        com.llmscannerseguridad.privacy.ProtectionScope.incomplete();
    }

    private void rememberId(String id, UnaryOperator<String> redact) {
        String safe = redact.apply(id);
        if (!safe.equals(id)) inspectionIncomplete = true;
        if (TargetRules.bytes(safe) <= 128) remoteIds.add(safe);
        else inspectionIncomplete = true;
    }

    private void incomplete(Source source) {
        if (availability.get(source) != TOO_LARGE) availability.put(source, INCOMPLETE);
    }

    private static Source source(Kind kind) {
        return switch (kind) {
            case QUERY, ITEM -> RETRIEVED_CONTEXT;
            case REQUEST -> TOOL_ARGUMENT;
            case RESULT -> TOOL_RESULT;
            case MODEL_TEXT, ANSWER -> FINAL_OUTPUT;
            case PROMPT -> USER_INPUT;
        };
    }

    private static int maximum(Kind kind) {
        return switch (kind) {
            case QUERY -> 1;
            case ITEM, MODEL_TEXT -> 32;
            default -> 16;
        };
    }

    private static Object select(Object value, List<Object> path) {
        for (Object segment : path) {
            if (segment instanceof String key && value instanceof Map<?, ?> map && map.containsKey(key)) value = map.get(key);
            else if (segment instanceof Integer index && value instanceof List<?> list && index < list.size()) value = list.get(index);
            else return ABSENT;
        }
        return value;
    }
}
