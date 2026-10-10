package com.llmscannerseguridad.application.reporting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.domain.execution.ExecutionStatus;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import com.llmscannerseguridad.domain.reporting.*;

public interface ReportInputPort {
    Inputs read(UUID scanId);
    record Execution(UUID id, UUID scanId, UUID testId, String code, int revision, SecurityCategory category,
            ExecutionStatus status, Instant finishedAt, UUID targetConfigurationId, Instant targetUpdatedAt) { }
    record Evaluation(UUID scanId, UUID testId, ReportEvaluation value) {
        @Override public String toString() { return "ReportInputEvaluation[id=" + value.id() + "]"; }
    }
    record Finding(UUID testId, ReportFinding value) {
        @Override public String toString() { return "ReportInputFinding[id=" + value.id() + "]"; }
    }
    record Inputs(Scan scan, String reportVersion, String scoringVersion, String evaluationProfile,
            ReportSummary summary, List<Execution> executions, List<Evaluation> evaluations, List<Finding> findings) {
        public Inputs { executions = List.copyOf(executions); evaluations = List.copyOf(evaluations); findings = List.copyOf(findings); }
        @Override public String toString() { return "ReportInputs[scanId=" + scan.id() + "]"; }
    }
}
