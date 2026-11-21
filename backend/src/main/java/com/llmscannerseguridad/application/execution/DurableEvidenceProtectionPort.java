package com.llmscannerseguridad.application.execution;

/** La implementación debe certificar ambos cuerpos antes de entregarlos a finish. */
public interface DurableEvidenceProtectionPort {
    InvocationResult protect(InvocationResult candidate, ScanBudget budget);
}
