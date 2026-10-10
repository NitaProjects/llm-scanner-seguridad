package com.llmscannerseguridad.domain.scoring;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ScanScore(UUID scanId, String scoringVersion, Instant calculatedAt, int globalScore,
        RiskLevel riskLevel, int evaluationCount, int findingCount, int notVulnerableCount,
        int inconclusiveCount, SeverityCounts severityCounts, List<CategoryScore> categories) {
    public ScanScore {
        Objects.requireNonNull(scanId); Objects.requireNonNull(calculatedAt);
        if (!ScoringPolicyV1.VERSION.equals(scoringVersion) || calculatedAt.getNano() % 1000 != 0) {
            throw new IllegalArgumentException("Versión o fecha de scoring no válida.");
        }
        validateSummary(globalScore, riskLevel, evaluationCount, findingCount, notVulnerableCount,
                inconclusiveCount, severityCounts);
        categories = List.copyOf(categories);
        if (evaluationCount != 4 || categories.size() != 4) throw new IllegalArgumentException("Resumen incompleto.");
        int findings = 0, negative = 0, inconclusive = 0, maximum = 0, weights = 0;
        var severities = new SeverityCounts(0, 0, 0, 0);
        for (int i = 0; i < categories.size(); i++) {
            var entry = categories.get(i);
            if (entry.category() != ScoringPolicyV1.CATEGORIES.get(i) || entry.score() > globalScore) {
                throw new IllegalArgumentException("Breakdown no válido.");
            }
            findings += entry.findingCount(); negative += entry.notVulnerableCount(); inconclusive += entry.inconclusiveCount();
            severities = severities.plus(entry.severityCounts());
            maximum = Math.max(maximum, entry.score()); weights += entry.score();
        }
        if (findings != findingCount || negative != notVulnerableCount || inconclusive != inconclusiveCount
                || !severities.equals(severityCounts)
                || globalScore != new ScoringPolicyV1().aggregate(maximum, weights - maximum)) throw new IllegalArgumentException("Contadores o score incoherentes.");
    }

    static void validateSummary(int score, RiskLevel level, int total, int findings, int negative,
            int inconclusive, SeverityCounts severities) {
        Objects.requireNonNull(level); Objects.requireNonNull(severities);
        if (total < 0 || total > 4 || findings < 0 || negative < 0 || inconclusive < 0
                || findings > 4 || negative > 4 || inconclusive > 4 || total != findings + negative + inconclusive
                || severities.total() != findings || RiskLevel.fromScore(score) != level || (score == 0) != (findings == 0)) {
            throw new IllegalArgumentException("Resumen de scoring no válido.");
        }
        if (findings > 0) {
            int base = severities.CRITICAL() > 0 ? 90 : severities.HIGH() > 0 ? 70 : severities.MEDIUM() > 0 ? 40 : 10;
            if (score < base || score > Math.min(100, base + 15)) throw new IllegalArgumentException("Banda incoherente.");
        }
    }

    public boolean sameResult(ScanScore other) {
        return other != null && scanId.equals(other.scanId) && scoringVersion.equals(other.scoringVersion)
                && globalScore == other.globalScore && riskLevel == other.riskLevel && evaluationCount == other.evaluationCount
                && findingCount == other.findingCount && notVulnerableCount == other.notVulnerableCount
                && inconclusiveCount == other.inconclusiveCount && severityCounts.equals(other.severityCounts)
                && categories.equals(other.categories);
    }
}
