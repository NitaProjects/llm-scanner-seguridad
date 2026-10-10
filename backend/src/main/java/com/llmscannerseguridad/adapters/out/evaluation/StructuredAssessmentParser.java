package com.llmscannerseguridad.adapters.out.evaluation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import com.llmscannerseguridad.application.evaluation.EvaluatorInput;
import com.llmscannerseguridad.domain.evaluation.*;

public final class StructuredAssessmentParser {
    private static final Set<String> FIELDS = Set.of("verdict","confidence","reasoningSummary","suggestedSeverity","recommendation","evidenceReferences");
    public LlmAssessment parse(String text, EvaluatorInput input) {
        var value = EvaluationJson.object(EvaluationJson.parse(text), FIELDS);
        var verdict = Verdict.valueOf(EvaluationJson.string(value.get("verdict")));
        Object number = value.get("confidence");
        BigDecimal confidence = number instanceof BigDecimal decimal ? decimal
                : number instanceof BigInteger integer ? new BigDecimal(integer) : null;
        var severity = value.get("suggestedSeverity") == null ? null : Severity.valueOf(EvaluationJson.string(value.get("suggestedSeverity")));
        if (!(value.get("evidenceReferences") instanceof List<?> list) || list.size() > 3) throw EvaluationJson.invalid();
        var refs = new ArrayList<EvidenceReference>();
        for (var item : list) {
            var ref = EvaluationJson.object(item, Set.of("evidenceId","start","end","criterionId"));
            var reference = new EvidenceReference(EvaluationJson.string(ref.get("evidenceId")), EvaluationJson.integer(ref.get("start")),
                    EvaluationJson.integer(ref.get("end")), EvaluationJson.string(ref.get("criterionId")));
            if (!reference.validFor(input.evidenceText(), input.criteria().criteriaId())) throw EvaluationJson.invalid();
            refs.add(reference);
        }
        // Validar longitud/forma antes de descartar narrativa no aprobada para publicación.
        new LlmAssessment(verdict, confidence, EvaluationJson.string(value.get("reasoningSummary")), severity,
                EvaluationJson.string(value.get("recommendation")), refs);
        return new LlmAssessment(verdict, confidence, safeSummary(verdict), severity,
                "Revisar la política de seguridad y el criterio del escenario sintético.", refs);
    }
    private String safeSummary(Verdict verdict) {
        return switch (verdict) {
            case VULNERABLE -> "El evaluador propone un incumplimiento sujeto a consolidación con evidencia independiente.";
            case NOT_VULNERABLE -> "El evaluador propone cumplimiento de la expectativa de esta prueba.";
            case INCONCLUSIVE -> "El evaluador no aporta una conclusión suficiente para esta prueba.";
        };
    }
}
