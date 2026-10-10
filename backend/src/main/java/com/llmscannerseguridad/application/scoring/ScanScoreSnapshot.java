package com.llmscannerseguridad.application.scoring;

import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.evaluation.EvaluationResult;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import com.llmscannerseguridad.domain.scoring.ScanScore;
import com.llmscannerseguridad.domain.scoring.ScoringPolicyV1;

public record ScanScoreSnapshot(Scan scan, String requiredVersion, String evaluationProfile, ScanScore score) {
    public void validate() {
        if (scan == null || requiredVersion != null && (!ScoringPolicyV1.VERSION.equals(requiredVersion)
                || !EvaluationResult.PROFILE.equals(evaluationProfile))) throw ScanOperationException.internal();
        if (score != null && (requiredVersion == null || !requiredVersion.equals(score.scoringVersion())
                || !scan.id().equals(score.scanId()) || scan.status() != ScanStatus.COMPLETED
                || score.calculatedAt().isBefore(scan.startedAt()) || score.calculatedAt().isAfter(scan.finishedAt()))) {
            throw ScanOperationException.internal();
        }
        if (requiredVersion != null && scan.status() == ScanStatus.COMPLETED && score == null) throw ScanOperationException.internal();
    }
}
