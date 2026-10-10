package com.llmscannerseguridad.domain.reporting;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import static com.llmscannerseguridad.domain.reporting.ReportRules.*;

public record ReportEvaluation(UUID id, UUID testExecutionId, String securityTestCode, int securityTestRevision,
        SecurityCategory category, EvaluationStatus status, Verdict finalVerdict, BigDecimal confidence,
        Severity severity, OutcomeReason outcomeReason, LlmStatus llmStatus, LlmError llmErrorReason,
        boolean degraded, boolean evidenceComplete, String criteriaId, String criteriaVersion, Versions versions,
        Instant finishedAt, UUID findingId) {
    public record Versions(String engineVersion, String policyVersion, String confidenceVersion, String severityVersion,
            String promptVersion, String schemaVersion, String llmInputProfile) {
        public Versions {
            for (var v : new String[]{engineVersion, policyVersion, confidenceVersion, severityVersion,
                    promptVersion, schemaVersion, llmInputProfile}) nullableText(v, 64);
        }
        @Override public String toString() { return "ReportVersions[protegidas]"; }
    }
    public ReportEvaluation {
        require(id != null && testExecutionId != null && status == EvaluationStatus.COMPLETED && finalVerdict != null
                && versions != null && llmStatus != null && llmStatus != LlmStatus.NOT_REQUESTED);
        test(securityTestCode, securityTestRevision, category); time(finishedAt); ReportRules.confidence(confidence);
        nullableText(criteriaId, 64); nullableText(criteriaVersion, 32);
        require((finalVerdict == Verdict.VULNERABLE) == (severity != null)
                && (finalVerdict == Verdict.VULNERABLE) == (findingId != null)
                && (finalVerdict != Verdict.INCONCLUSIVE || confidence.signum() == 0 && outcomeReason != null)
                && (llmStatus != LlmStatus.SUCCEEDED || llmErrorReason == null)
                && (llmStatus != LlmStatus.FAILED || llmErrorReason != null)
                && (llmStatus == LlmStatus.SUCCEEDED || degraded));
    }
    @Override public String toString() { return "ReportEvaluation[id=" + id + ", verdict=" + finalVerdict + "]"; }
}
