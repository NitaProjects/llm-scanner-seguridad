package com.llmscannerseguridad.application.evaluation;

import java.util.UUID;
import com.llmscannerseguridad.application.scan.GetScanUseCase;

public final class ListEvaluationsUseCase {
    private final GetScanUseCase scans;
    private final EvaluationRepositoryPort evaluations;
    public ListEvaluationsUseCase(GetScanUseCase scans, EvaluationRepositoryPort evaluations) {
        this.scans = scans; this.evaluations = evaluations;
    }
    public EvaluationListing execute(UUID projectId, UUID scanId) {
        scans.execute(projectId, scanId);
        return evaluations.listEvaluations(scanId);
    }
}
