package com.llmscannerseguridad.application.evaluation;

import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.application.scan.GetScanUseCase;

public final class ListScanFindingsUseCase {
    private final GetScanUseCase scans;
    private final EvaluationRepositoryPort evaluations;
    public ListScanFindingsUseCase(GetScanUseCase scans, EvaluationRepositoryPort evaluations) {
        this.scans = scans; this.evaluations = evaluations;
    }
    public List<FindingMetadata> execute(UUID projectId, UUID scanId) {
        scans.execute(projectId, scanId);
        return evaluations.listFindings(scanId);
    }
}
