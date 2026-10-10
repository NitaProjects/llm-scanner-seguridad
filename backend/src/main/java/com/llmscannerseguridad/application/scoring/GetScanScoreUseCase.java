package com.llmscannerseguridad.application.scoring;

import java.util.UUID;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.scan.ScanStatus;

public final class GetScanScoreUseCase {
    private final ProjectRepositoryPort projects;
    private final ScanScoreRepositoryPort scores;
    public GetScanScoreUseCase(ProjectRepositoryPort projects, ScanScoreRepositoryPort scores) {
        this.projects = projects; this.scores = scores;
    }
    public ScoreListing execute(UUID projectId, UUID scanId) {
        if (projects.findById(projectId).isEmpty()) throw new ScanOperationException(ScanOperationException.Reason.PROJECT_NOT_FOUND);
        var snapshot = scores.findScoped(projectId, scanId).orElseThrow(() -> new ScanOperationException(ScanOperationException.Reason.SCAN_NOT_FOUND));
        snapshot.validate();
        if (snapshot.score() != null) return new ScoreListing(ScoreAvailability.AVAILABLE, snapshot.score());
        var availability = snapshot.requiredVersion() == null ? ScoreAvailability.NOT_APPLICABLE
                : snapshot.scan().status() == ScanStatus.FAILED ? ScoreAvailability.UNAVAILABLE : ScoreAvailability.PENDING;
        return new ScoreListing(availability, null);
    }
}
