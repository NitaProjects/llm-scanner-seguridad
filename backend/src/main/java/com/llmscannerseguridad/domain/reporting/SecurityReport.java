package com.llmscannerseguridad.domain.reporting;

import java.time.Instant;
import java.util.*;
import com.llmscannerseguridad.domain.evaluation.*;
import static com.llmscannerseguridad.domain.reporting.ReportRules.*;

public record SecurityReport(UUID reportId, UUID scanId, UUID projectId, Instant generatedAt, String reportVersion,
        String locale, Scope scope, ReportSummary summary, List<ReportEvaluation> evaluations,
        List<ReportFinding> findings, Interpretation interpretation, List<Limitation> limitations) {
    public record Scope(Instant scanCreatedAt, Instant scanStartedAt, UUID targetConfigurationId, Instant targetUpdatedAt) {
        public Scope {
            time(scanCreatedAt); time(scanStartedAt); time(targetUpdatedAt);
            require(targetConfigurationId != null && !scanStartedAt.isBefore(scanCreatedAt));
        }
    }
    public record Interpretation(String scoreMeaning, String confidenceMeaning, String verdictMeaning, String technicalFailureMeaning) {
        public Interpretation { text(scoreMeaning,1000); text(confidenceMeaning,1000); text(verdictMeaning,1000); text(technicalFailureMeaning,1000); }
        @Override public String toString() { return "ReportInterpretation[protegida]"; }
    }
    public record Limitation(String code, String text) {
        public Limitation { ReportRules.text(code,64); require(code.matches("[A-Z0-9_]+")); ReportRules.text(text,1000); }
        @Override public String toString() { return "ReportLimitation[code=" + code + "]"; }
    }
    public SecurityReport {
        require(reportId != null && scanId != null && projectId != null && VERSION.equals(reportVersion)
                && LOCALE.equals(locale) && scope != null && summary != null && interpretation != null);
        time(generatedAt);
        evaluations = List.copyOf(evaluations); findings = List.copyOf(findings); limitations = List.copyOf(limitations);
        require(evaluations.size() == 4 && findings.size() <= 4 && limitations.size() == 9
                && scanId.equals(summary.scanId()) && !generatedAt.isBefore(summary.calculatedAt())
                && !summary.calculatedAt().isBefore(scope.scanStartedAt()) && summary.findingCount() == findings.size());
        var ids = new HashSet<UUID>(); var executionIds = new HashSet<UUID>(); var findingIds = new HashSet<UUID>();
        var ordered = evaluations.stream().sorted(Comparator.comparing(ReportEvaluation::securityTestCode)
                .thenComparingInt(ReportEvaluation::securityTestRevision).thenComparing(e -> e.id().toString())).toList();
        require(ordered.equals(evaluations) && findings.stream().sorted(ReportFinding.ORDER).toList().equals(findings));
        for (int i = 0; i < 4; i++) {
            var e = evaluations.get(i); var c = summary.categories().get(i);
            require(ids.add(e.id()) && executionIds.add(e.testExecutionId()) && e.category() == CATEGORIES.get(i)
                    && !e.finishedAt().isBefore(scope.scanStartedAt()) && !summary.calculatedAt().isBefore(e.finishedAt())
                    && c.findingCount() == (e.finalVerdict() == Verdict.VULNERABLE ? 1 : 0)
                    && c.notVulnerableCount() == (e.finalVerdict() == Verdict.NOT_VULNERABLE ? 1 : 0)
                    && c.inconclusiveCount() == (e.finalVerdict() == Verdict.INCONCLUSIVE ? 1 : 0));
            var matching = findings.stream().filter(f -> f.evaluationResultId().equals(e.id())).toList();
            require(matching.size() == c.findingCount());
            int low = 0, medium = 0, high = 0, critical = 0;
            if (!matching.isEmpty()) {
                var f = matching.getFirst();
                require(findingIds.add(f.id()) && f.id().equals(e.findingId()) && f.scanId().equals(scanId)
                        && f.testExecutionId().equals(e.testExecutionId()) && f.securityTestCode().equals(e.securityTestCode())
                        && f.securityTestRevision() == e.securityTestRevision() && f.category() == e.category()
                        && f.severity() == e.severity() && f.confidence().compareTo(e.confidence()) == 0
                        && !f.createdAt().isBefore(e.finishedAt()) && !summary.calculatedAt().isBefore(f.createdAt()));
                switch (f.severity()) { case LOW -> low++; case MEDIUM -> medium++; case HIGH -> high++; case CRITICAL -> critical++; }
            }
            require(c.severityCounts().equals(new ReportSummary.Counts(low,medium,high,critical)));
        }
        require(findingIds.size() == findings.size() && limitations.stream().map(Limitation::code).distinct().count() == 9);
    }
    public boolean sameContent(SecurityReport other) {
        return other != null && scanId.equals(other.scanId) && projectId.equals(other.projectId)
                && reportVersion.equals(other.reportVersion) && locale.equals(other.locale) && scope.equals(other.scope)
                && summary.equals(other.summary) && evaluations.equals(other.evaluations) && findings.equals(other.findings)
                && interpretation.equals(other.interpretation) && limitations.equals(other.limitations);
    }
    @Override public String toString() { return "SecurityReport[id=" + reportId + ", scanId=" + scanId + ", version=" + reportVersion + "]"; }
}
