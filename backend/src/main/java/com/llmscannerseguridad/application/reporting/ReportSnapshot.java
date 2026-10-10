package com.llmscannerseguridad.application.reporting;

import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.scan.*;
import com.llmscannerseguridad.domain.reporting.*;

public record ReportSnapshot(Scan scan, String reportVersion, String scoringVersion, String evaluationProfile,
        ReportSummary summary, SecurityReport report) {
    public void validate() {
        try {
            ReportRules.require(scan != null && (scoringVersion == null || "scoring-v1".equals(scoringVersion)
                    && "evaluation-engine-v1".equals(evaluationProfile))
                    && (reportVersion == null || ReportRules.VERSION.equals(reportVersion) && "scoring-v1".equals(scoringVersion)
                        && "evaluation-engine-v1".equals(evaluationProfile)));
            if (summary != null) ReportRules.require(scoringVersion != null && scoringVersion.equals(summary.scoringVersion())
                    && scan.id().equals(summary.scanId()) && scan.status() == ScanStatus.COMPLETED
                    && !summary.calculatedAt().isBefore(scan.startedAt()) && !summary.calculatedAt().isAfter(scan.finishedAt()));
            if (scoringVersion != null && scan.status() == ScanStatus.COMPLETED) ReportRules.require(summary != null);
            if (report != null) ReportRules.require(reportVersion != null && reportVersion.equals(report.reportVersion())
                    && summary != null && scan.status() == ScanStatus.COMPLETED && report.scanId().equals(scan.id())
                    && report.projectId().equals(scan.projectId()) && report.summary().equals(summary)
                    && report.scope().scanCreatedAt().equals(scan.createdAt()) && report.scope().scanStartedAt().equals(scan.startedAt())
                    && !report.generatedAt().isAfter(scan.finishedAt()));
            if (reportVersion != null && scan.status() == ScanStatus.COMPLETED) ReportRules.require(report != null);
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
}
