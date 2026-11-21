package com.llmscannerseguridad.privacy;

import com.llmscannerseguridad.domain.evaluation.*;
import tools.jackson.databind.json.JsonMapper;

/** Conserva identidad/decisión y construye la única fuente de EvaluationResult y Finding. */
public final class DurableEvaluationProtection {
    public EvaluationResult protect(EvaluationResult r, int limit) {
        ProtectionScope.requireScan(r.scanId());
        var json = JsonMapper.shared();
        // No se pueden cambiar referencias, criterios, configuración o señales para salvar un write.
        DurableEvidenceProtection.requireDocument(json.writeValueAsString(configurationView(r.configuration())), limit);
        DurableEvidenceProtection.requireDocument(json.writeValueAsString(r.deterministicSignals()), limit);
        var component = r.llm();
        DurableEvidenceProtection.requireDocument(json.writeValueAsString(component.assessment()), limit);
        var llm = new EvaluationResult.LlmComponent(component.status(), component.errorReason(), component.attemptCount(),
                component.assessment(), DurableEvidenceProtection.text(component.resolvedModel(), limit), component.latencyMs(),
                component.inputTokens(), component.outputTokens(), component.reservedCostUsd(), component.estimatedCostUsd(),
                DurableEvidenceProtection.text(component.pricingVersion(), limit), DurableEvidenceProtection.text(component.estimateVersion(), limit),
                DurableEvidenceProtection.text(component.providerRequestId(), limit));
        var evidence = r.evidence();
        if (evidence.projection() == null && evidence.inputHash() != null) evidence = evidence.withoutProjection();
        if (evidence.projection() != null) {
            var state = new ProtectedContentInspector().inspectDocument(evidence.projection(), ProtectionScope.values(), limit).state();
            if (state != ProtectedContentInspector.State.SAFE) {
                evidence = new EvaluationResult.Evidence(null, false, true, 0, evidence.evidenceBytes(), null);
            }
        }
        var safe = new EvaluationResult(r.id(), r.testExecutionId(), r.scanId(), r.securityTestId(), r.category(), r.status(),
                r.createdAt(), r.startedAt(), r.finishedAt(), r.errorReason(), r.decision(), r.deterministicSignals(), llm,
                r.configuration(), evidence, DurableEvidenceProtection.text(r.reasoningSummary(), limit),
                DurableEvidenceProtection.text(r.recommendation(), limit), DurableEvidenceProtection.text(r.evidenceSummary(), limit));
        // Incluye metadata, referencias y representaciones JSON antes de que exista ningún parámetro SQL.
        @SuppressWarnings("unchecked")
        var logical = (java.util.Map<String, Object>) json.readValue(json.writeValueAsString(safe), java.util.Map.class);
        logical.put("configuration", configurationView(safe.configuration()));
        @SuppressWarnings("unchecked")
        var evidenceView = (java.util.Map<String, Object>) logical.get("evidence");
        if (safe.evidence().projection() != null) evidenceView.put("projection", json.readValue(safe.evidence().projection(), Object.class));
        DurableEvidenceProtection.requireDocument(json.writeValueAsString(logical), limit);
        return safe;
    }
    private Object configurationView(EvaluationResult.Configuration configuration) {
        if (configuration == null) return null;
        var json = JsonMapper.shared();
        @SuppressWarnings("unchecked")
        var view = (java.util.Map<String, Object>) json.readValue(json.writeValueAsString(configuration), java.util.Map.class);
        if (configuration.configurationSnapshot() != null) view.put("configurationSnapshot", json.readValue(configuration.configurationSnapshot(), Object.class));
        if (configuration.samplingParameters() != null) view.put("samplingParameters", json.readValue(configuration.samplingParameters(), Object.class));
        return view;
    }
}
