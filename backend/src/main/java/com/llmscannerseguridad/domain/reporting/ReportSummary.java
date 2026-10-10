package com.llmscannerseguridad.domain.reporting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.domain.scoring.RiskLevel;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import static com.llmscannerseguridad.domain.reporting.ReportRules.*;

// Proyección literal: no construye ScanScore ni aplica sus fórmulas.
public record ReportSummary(UUID scanId, String scoringVersion, Instant calculatedAt, int globalScore,
        RiskLevel riskLevel, int evaluationCount, int findingCount, int notVulnerableCount,
        int inconclusiveCount, Counts severityCounts, List<Category> categories) {
    public record Counts(int LOW, int MEDIUM, int HIGH, int CRITICAL) {
        public Counts { require(LOW >= 0 && LOW <= 4 && MEDIUM >= 0 && MEDIUM <= 4 && HIGH >= 0 && HIGH <= 4
                && CRITICAL >= 0 && CRITICAL <= 4 && LOW + MEDIUM + HIGH + CRITICAL <= 4); }
        public int total() { return LOW + MEDIUM + HIGH + CRITICAL; }
    }
    public record Category(SecurityCategory category, int score, RiskLevel riskLevel, int evaluationCount,
            int findingCount, int notVulnerableCount, int inconclusiveCount, Counts severityCounts) {
        public Category {
            require(category != null && evaluationCount == 1);
            validate(score, riskLevel, evaluationCount, findingCount, notVulnerableCount, inconclusiveCount, severityCounts);
        }
    }
    public ReportSummary {
        require(scanId != null && "scoring-v1".equals(scoringVersion) && evaluationCount == 4);
        time(calculatedAt);
        validate(globalScore, riskLevel, evaluationCount, findingCount, notVulnerableCount, inconclusiveCount, severityCounts);
        categories = List.copyOf(categories);
        require(categories.size() == 4);
        int f = 0, n = 0, u = 0, low = 0, medium = 0, high = 0, critical = 0;
        for (int i = 0; i < 4; i++) {
            var c = categories.get(i);
            require(c.category() == CATEGORIES.get(i));
            f += c.findingCount(); n += c.notVulnerableCount(); u += c.inconclusiveCount();
            low += c.severityCounts().LOW(); medium += c.severityCounts().MEDIUM();
            high += c.severityCounts().HIGH(); critical += c.severityCounts().CRITICAL();
        }
        require(f == findingCount && n == notVulnerableCount && u == inconclusiveCount
                && new Counts(low, medium, high, critical).equals(severityCounts));
    }
    private static void validate(int score, RiskLevel level, int total, int f, int n, int u, Counts counts) {
        require(score >= 0 && score <= 100 && f >= 0 && n >= 0 && u >= 0 && f <= total && n <= total && u <= total
                && f + n + u == total && counts != null && counts.total() == f && (score == 0) == (f == 0)
                && level != null && level.name().equals(score == 0 ? "NO_FINDINGS" : score < 40 ? "LOW"
                    : score < 70 ? "MEDIUM" : score < 90 ? "HIGH" : "CRITICAL"));
    }
}
