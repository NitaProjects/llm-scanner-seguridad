package com.llmscannerseguridad.privacy;

import java.util.*;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import tools.jackson.databind.json.JsonMapper;

/** Política de sustitución; todas las decisiones N4 proceden del mismo inspector. */
public final class DurableEvidenceProtection implements DurableEvidenceProtectionPort {
    private static final ProtectedContentInspector INSPECTOR = new ProtectedContentInspector();
    private final int requestBytes, textBytes, evidenceBytes;
    public DurableEvidenceProtection(int requestBytes, int textBytes, int evidenceBytes) {
        this.requestBytes = requestBytes; this.textBytes = textBytes; this.evidenceBytes = evidenceBytes;
    }
    public DurableEvidenceProtection() { this(65536, 65536, 131072); }

    @Override public InvocationResult protect(InvocationResult raw, ScanBudget budget) {
        if (raw.requestBody() == null && raw.responseText() == null) {
            if (ProtectionScope.complete()) return raw;
            return new InvocationResult(raw.httpStatus(), raw.latencyMs(), ExecutionError.EVIDENCE_LIMIT_EXCEEDED, null, null, false, false);
        }
        try {
            if (!ProtectionScope.complete() || budget != null && budget.remainingNanos() == 0) throw ProtectedContentInspector.incomplete();
            String request = raw.requestBody();
            if (request != null) {
                var state = INSPECTOR.inspectDocument(request, ProtectionScope.values(), requestBytes).state();
                if (state == ProtectedContentInspector.State.UNINSPECTABLE) throw ProtectedContentInspector.incomplete();
                if (state == ProtectedContentInspector.State.MATCH) request = INSPECTOR.redactDocument(request, ProtectionScope.values(), requestBytes);
                requireDocument(request, requestBytes);
            }
            String response = text(raw.responseText(), textBytes);
            // encodeResponse es un wrapper del scanner, no una quinta capa atacante.
            if (response != null) requireDocument(JsonMapper.shared().writeValueAsString(response), evidenceBytes);
            return new InvocationResult(raw.httpStatus(), raw.latencyMs(), raw.errorReason(), request, response,
                    request != null && (raw.requestRedacted() || !Objects.equals(request, raw.requestBody())),
                    response != null && (raw.responseRedacted() || !Objects.equals(response, raw.responseText())));
        } catch (RuntimeException error) {
            // Se omite toda evidencia, preservando precedencia de cancelación/deadline ya acreditada.
            ExecutionError reason = raw.errorReason() == ExecutionError.SCAN_BUDGET_EXCEEDED
                    || raw.errorReason() == ExecutionError.EXECUTION_INTERRUPTED
                    ? raw.errorReason() : ExecutionError.EVIDENCE_LIMIT_EXCEEDED;
            return new InvocationResult(raw.httpStatus(), raw.latencyMs(), reason,
                    null, null, false, false);
        }
    }

    public static String text(String value, int maxBytes) {
        if (!ProtectionScope.complete()) throw ProtectedContentInspector.incomplete();
        return INSPECTOR.redact(value, ProtectionScope.values(), maxBytes);
    }

    public static String redact(String value, Collection<String> additional, int maxBytes) {
        if (!ProtectionScope.complete()) throw ProtectedContentInspector.incomplete();
        var values = new LinkedHashSet<>(ProtectionScope.values());
        additional.stream().filter(Objects::nonNull).filter(v -> !v.isEmpty()).forEach(values::add);
        return INSPECTOR.redact(value, values, maxBytes);
    }

    /** Campos estructurales/obligatorios: nunca mutar identidad ni contrato para admitir el write. */
    public static void requireDocument(String value, int maxBytes) {
        if (!ProtectionScope.complete() || INSPECTOR.inspectDocument(value, ProtectionScope.values(), maxBytes).state()
                != ProtectedContentInspector.State.SAFE) throw ProtectedContentInspector.incomplete();
    }
    public static void requireText(String value, int maxBytes) {
        if (!ProtectionScope.complete() || value != null && value.indexOf(0) >= 0
                || INSPECTOR.inspect(value, ProtectionScope.values(), maxBytes).state() != ProtectedContentInspector.State.SAFE) {
            throw ProtectedContentInspector.incomplete();
        }
    }
}
