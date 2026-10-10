package com.llmscannerseguridad.adapters.out.evaluation;

import java.util.List;
import java.util.Map;
import com.llmscannerseguridad.application.evaluation.EvaluatorInput;

public final class EvaluatorMessages {
    private final String system = EvaluationJson.resource("evaluator-prompt-v1.txt", 8192);
    private final Object schema = EvaluationJson.parse(EvaluationJson.resource("evaluator-output-v1.json", 8192));
    public String promptHash() { return EvaluationJson.hash(system); }
    public String schemaHash() { return EvaluationJson.hash(EvaluationJson.canonical(schema)); }
    public String request(EvaluatorInput input, EvaluatorSettings settings) {
        var trusted = Map.of("schemaVersion", 1, "criteria", input.criteria(), "catalogPrompt", input.catalogPrompt(),
                "trustedSyntheticContext", input.trustedSyntheticContext());
        var data = Map.of("provenance", "UNTRUSTED_TARGET_DATA", "deterministicSignals", input.deterministicSignals(),
                "evidence", List.of(Map.of("evidenceId", "response", "text", input.evidenceText(),
                    "complete", input.complete(), "redacted", input.redacted())));
        return EvaluationJson.write(Map.of("model", settings.model(), "store", false,
                "max_output_tokens", settings.maxOutputTokens(), "input", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "developer", "content", EvaluationJson.canonical(trusted)),
                    Map.of("role", "user", "content", EvaluationJson.canonical(data))),
                "text", Map.of("format", Map.of("type", "json_schema", "name", "evaluator_output_v1", "strict", true, "schema", schema))));
    }
}
