package com.llmscannerseguridad.adapters.out.evaluation;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.llmscannerseguridad.application.evaluation.EvaluatorInput;
import com.llmscannerseguridad.application.evaluation.SemanticProjection;

public final class EvaluatorMessages {
    private final String system = EvaluationJson.resource("evaluator-prompt-v1.txt", 8192);
    private final Object schema = EvaluationJson.parse(EvaluationJson.resource("evaluator-output-v1.json", 8192));
    public String promptHash() { return EvaluationJson.hash(system); }
    public String schemaHash() { return EvaluationJson.hash(EvaluationJson.canonical(schema)); }
    public String request(EvaluatorInput input, EvaluatorSettings settings) {
        var payload = payload(input);
        return EvaluationJson.write(Map.of("model", settings.model(), "store", false,
                "max_output_tokens", settings.maxOutputTokens(), "input", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "developer", "content", EvaluationJson.canonical(payload.trusted())),
                    Map.of("role", "user", "content", EvaluationJson.canonical(payload.data()))),
                "text", Map.of("format", Map.of("type", "json_schema", "name", "evaluator_output_v1", "strict", true, "schema", schema))));
    }

    String projection(EvaluatorInput input) {
        if (!input.additionalSources().isEmpty()) return EvaluationJson.canonical(payload(input));
        if (input.semanticProjection() != null) return EvaluationJson.canonical(semantic(input));
        // Mantener la representación canónica y el hash de los inputs v1.
        return EvaluationJson.canonical(Map.of("criteria", input.criteria(), "catalogPrompt", input.catalogPrompt(),
                "trustedSyntheticContext", input.trustedSyntheticContext(), "deterministicSignals", input.deterministicSignals(),
                "evidenceText", input.evidenceText(), "complete", input.complete(), "redacted", input.redacted()));
    }

    private record Payload(Map<String, Object> trusted, Map<String, Object> data) { }

    private Payload payload(EvaluatorInput input) {
        var payload = input.semanticProjection() == null ? legacy(input) : semantic(input);
        if (input.additionalSources().isEmpty()) return payload;
        var data = new HashMap<>(payload.data());
        data.put("observations", input.additionalSources().stream().map(source -> {
            var fields = new HashMap<String, Object>(Map.of(
                    "source", source.source(), "kind", source.kind(), "ordinal", source.ordinal(), "text", source.value()));
            if (source.relatedOrdinal() != null) fields.put("relatedOrdinal", source.relatedOrdinal());
            if (source.alias() != null) fields.put("alias", source.alias());
            return fields;
        }).toList());
        return new Payload(payload.trusted(), data);
    }

    private Payload legacy(EvaluatorInput input) {
        var trusted = Map.of("schemaVersion", 1, "criteria", input.criteria(), "catalogPrompt", input.catalogPrompt(),
                "trustedSyntheticContext", input.trustedSyntheticContext());
        var data = Map.of("provenance", "UNTRUSTED_TARGET_DATA", "deterministicSignals", input.deterministicSignals(),
                "evidence", List.of(Map.of("evidenceId", "response", "text", input.evidenceText(),
                    "complete", input.complete(), "redacted", input.redacted())));
        return new Payload(trusted, data);
    }

    private Payload semantic(EvaluatorInput input) {
        var projection = input.semanticProjection();
        Map<String, Object> trusted = new HashMap<>();
        trusted.put("schemaVersion", 1);
        trusted.put("criteria", projection.criterion());
        if (projection.catalogPrompt() != null) trusted.put("catalogPrompt", projection.catalogPrompt());
        if (!(projection.parameters() instanceof SemanticProjection.NoParameters)) {
            trusted.put("trustedSyntheticContext", projection.parameters());
        }
        var data = Map.<String, Object>of("provenance", "UNTRUSTED_TARGET_DATA", "deterministicSignals", projection.signals(),
                "evidence", List.of(Map.of("evidenceId", "response", "text", projection.response())));
        return new Payload(trusted, data);
    }
}
