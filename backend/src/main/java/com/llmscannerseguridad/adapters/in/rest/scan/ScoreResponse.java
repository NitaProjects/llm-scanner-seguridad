package com.llmscannerseguridad.adapters.in.rest.scan;

import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.application.scoring.ScoreAvailability;
import com.llmscannerseguridad.application.scoring.ScoreListing;
import com.llmscannerseguridad.domain.scoring.*;

public record ScoreResponse(ScoreAvailability availability, Result score) {
    public record Result(UUID scanId, String scoringVersion, String calculatedAt, int globalScore, RiskLevel riskLevel,
            int evaluationCount, int findingCount, int notVulnerableCount, int inconclusiveCount,
            SeverityCounts severityCounts, List<CategoryScore> categories) { }
    public static ScoreResponse from(ScoreListing listing) {
        var score = listing.score();
        return new ScoreResponse(listing.availability(), score == null ? null : new Result(score.scanId(),score.scoringVersion(),
                new DateTimeFormatterBuilder().appendInstant(6).toFormatter().format(score.calculatedAt()),score.globalScore(),score.riskLevel(),
                score.evaluationCount(),score.findingCount(),score.notVulnerableCount(),score.inconclusiveCount(),score.severityCounts(),score.categories()));
    }
}
