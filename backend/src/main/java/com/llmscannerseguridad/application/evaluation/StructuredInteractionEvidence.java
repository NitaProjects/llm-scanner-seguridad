package com.llmscannerseguridad.application.evaluation;

import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Acompañante efímero. Sus valores no forman parte de las filas ni DTO de ejecución. */
public record StructuredInteractionEvidence(UUID executionId, List<Observation> observations,
        Map<Source, Availability> availability, Map<Source, Boolean> semanticSources, boolean inspectionIncomplete,
        Set<String> localCorrelationIds) {
    public enum Source { USER_INPUT, RETRIEVED_CONTEXT, TOOL_ARGUMENT, TOOL_RESULT, FINAL_OUTPUT }
    public enum Availability { COMPLETE, INCOMPLETE, TOO_LARGE }
    public enum Kind { QUERY, ITEM, REQUEST, RESULT, PROMPT, ANSWER, MODEL_TEXT }

    public record Observation(Source source, Kind kind, int ordinal, Integer relatedOrdinal,
            String alias, String value, boolean exportable, boolean complete) {
        @Override public String toString() { return "Observation[protegida]"; }
    }

    public StructuredInteractionEvidence {
        observations = List.copyOf(observations);
        availability = Map.copyOf(availability);
        semanticSources = Map.copyOf(semanticSources);
        localCorrelationIds = Set.copyOf(localCorrelationIds);
    }

    public Set<String> localValues() {
        var values = new HashSet<>(localCorrelationIds);
        for (var observation : observations) {
            if (observation.exportable() && observation.complete()
                    && availability.get(observation.source()) == Availability.COMPLETE
                    && semanticSources.containsKey(observation.source())) continue;
            values.add(observation.value());
            if (observation.alias() != null) values.add(observation.alias());
        }
        return values;
    }

    public StructuredInteractionEvidence withIncompleteInspection() {
        return new StructuredInteractionEvidence(executionId, observations, availability, semanticSources, true, localCorrelationIds);
    }

    public record Context(StructuredInteractionEvidence evidence, Observation userInput, Observation finalOutput,
            boolean finalOutputRedacted) {
        public Context(StructuredInteractionEvidence evidence, Observation userInput, Observation finalOutput) {
            this(evidence, userInput, finalOutput, false);
        }
        @Override public String toString() { return "ObservationContext[protegido]"; }
    }

    @Override public String toString() { return "StructuredInteractionEvidence[protegida]"; }
}
