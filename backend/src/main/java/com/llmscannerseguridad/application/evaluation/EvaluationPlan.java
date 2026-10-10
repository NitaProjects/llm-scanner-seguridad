package com.llmscannerseguridad.application.evaluation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

// Proyección de preparación: no transporta TargetConfiguration, cuerpos ni credenciales.
public record EvaluationPlan(UUID projectId, UUID scanId, UUID targetConfigurationId,
        Instant targetUpdatedAt, Instant scanStartedAt, List<Item> items) {
    public record Item(UUID testExecutionId, UUID securityTestId, String securityTestCode,
            int securityTestRevision, SecurityCategory category) { }
    public EvaluationPlan { items = List.copyOf(items); }
}
