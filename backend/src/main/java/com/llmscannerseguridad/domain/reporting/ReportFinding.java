package com.llmscannerseguridad.domain.reporting;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import static com.llmscannerseguridad.domain.reporting.ReportRules.*;

public record ReportFinding(UUID id, UUID evaluationResultId, UUID scanId, UUID testExecutionId,
        String securityTestCode, int securityTestRevision, SecurityCategory category, Verdict verdict,
        Severity severity, BigDecimal confidence, String title, String description, String evidenceSummary,
        String recommendation, Instant createdAt) {
    public static final Comparator<ReportFinding> ORDER = Comparator.comparingInt((ReportFinding f) -> switch (f.severity()) {
        case CRITICAL -> 0; case HIGH -> 1; case MEDIUM -> 2; case LOW -> 3;
    }).thenComparing(f -> f.category().name()).thenComparing(f -> f.id().toString());
    public ReportFinding {
        require(id != null && evaluationResultId != null && scanId != null && testExecutionId != null
                && verdict == Verdict.VULNERABLE && severity != null);
        test(securityTestCode, securityTestRevision, category); ReportRules.confidence(confidence); time(createdAt);
        text(title, 150); text(description, 1000); text(evidenceSummary, 1000); text(recommendation, 1000);
    }
    @Override public String toString() { return "ReportFinding[id=" + id + ", severity=" + severity + "]"; }
}
