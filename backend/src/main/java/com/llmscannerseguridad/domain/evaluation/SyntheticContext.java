package com.llmscannerseguridad.domain.evaluation;

import java.util.List;
import java.util.Map;

// Parámetros cerrados de las cinco reglas, sin scripts ni patrones ejecutables.
public record SyntheticContext(String reservedValue, String hiddenContext, List<String> hiddenAnchors,
        List<String> publicValues, String restriction, String actionMarker, boolean markerViolationConfirmed,
        List<String> expectedSafeResponses, OutputContract outputContract,
        boolean highImpact, String impactDescription, Severity limitedImpact) {
    public enum ValueType { STRING, NUMBER, BOOLEAN, NULL, OBJECT, ARRAY }
    public record OutputContract(List<String> forbiddenLiterals, List<String> requiredKeys,
            List<String> allowedKeys, Map<String, ValueType> propertyTypes, boolean securityRelevant) {
        public OutputContract {
            forbiddenLiterals = List.copyOf(forbiddenLiterals);
            requiredKeys = List.copyOf(requiredKeys);
            allowedKeys = List.copyOf(allowedKeys);
            propertyTypes = Map.copyOf(propertyTypes);
            if (!allowedKeys.containsAll(requiredKeys) || !allowedKeys.containsAll(propertyTypes.keySet())
                    || forbiddenLiterals.stream().anyMatch(String::isEmpty)) throw new IllegalArgumentException("Contrato de salida no válido.");
        }
    }
    public SyntheticContext {
        hiddenAnchors = List.copyOf(hiddenAnchors);
        publicValues = List.copyOf(publicValues);
        expectedSafeResponses = List.copyOf(expectedSafeResponses);
        if (hiddenAnchors.size() != 0 && hiddenAnchors.size() != 2
                || limitedImpact != null && limitedImpact != Severity.LOW && limitedImpact != Severity.MEDIUM
                || highImpact && (impactDescription == null || impactDescription.isBlank())) {
            throw new IllegalArgumentException("Contexto sintético no válido.");
        }
        if (hiddenAnchors.size() == 2) {
            if (hiddenContext == null || hiddenAnchors.get(0).equals(hiddenAnchors.get(1))) throw new IllegalArgumentException("Anclas no válidas.");
            var clauses = hiddenContext.split("[.;\\n]");
            var positions = new java.util.HashSet<Integer>();
            for (var anchor : hiddenAnchors) {
                int found = -1;
                if (anchor.codePointCount(0, anchor.length()) < 12 || anchor.codePointCount(0, anchor.length()) > 240) throw new IllegalArgumentException("Anclas no válidas.");
                for (int i = 0; i < clauses.length; i++) if (clauses[i].contains(anchor)) { if (found != -1) throw new IllegalArgumentException("Anclas no válidas."); found = i; }
                if (found < 0 || !positions.add(found)) throw new IllegalArgumentException("Anclas no válidas.");
            }
        }
    }
    @Override public String toString() { return "SyntheticContext[protegido]"; }
}
