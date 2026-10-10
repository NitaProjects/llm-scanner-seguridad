package com.llmscannerseguridad.domain.scoring;

import java.util.Objects;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

public record CategoryScore(SecurityCategory category, int score, RiskLevel riskLevel, int evaluationCount,
        int findingCount, int notVulnerableCount, int inconclusiveCount, SeverityCounts severityCounts) {
    public CategoryScore {
        Objects.requireNonNull(category);
        ScanScore.validateSummary(score, riskLevel, evaluationCount, findingCount, notVulnerableCount,
                inconclusiveCount, severityCounts);
        if (evaluationCount != 1) throw new IllegalArgumentException("La categoría requiere una evaluación.");
        if (findingCount == 1) {
            int base = severityCounts.CRITICAL() > 0 ? 90 : severityCounts.HIGH() > 0 ? 70 : severityCounts.MEDIUM() > 0 ? 40 : 10;
            if (score > base + 5) throw new IllegalArgumentException("Score de categoría no válido.");
        }
    }
}
