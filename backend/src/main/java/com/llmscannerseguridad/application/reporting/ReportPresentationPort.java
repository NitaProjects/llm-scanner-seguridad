package com.llmscannerseguridad.application.reporting;

import java.util.List;
import java.util.Map;
import com.llmscannerseguridad.domain.reporting.SecurityReport;

public interface ReportPresentationPort {
    Presentation load(String version, String locale);
    record Presentation(String reportVersion, String locale, SecurityReport.Interpretation interpretation,
            List<SecurityReport.Limitation> limitations, Labels labels) {
        public Presentation { limitations = List.copyOf(limitations); }
        @Override public String toString() { return "ReportPresentation[version=" + reportVersion + "]"; }
    }
    record Labels(Map<String,String> severity, Map<String,String> riskLevel, Map<String,String> verdict,
            Map<String,String> category, Map<String,String> status, Map<String,String> llmStatus,
            Map<String,String> outcomeReason, Map<String,String> llmErrorReason, Map<String,String> sections) {
        public Labels {
            severity = Map.copyOf(severity); riskLevel = Map.copyOf(riskLevel); verdict = Map.copyOf(verdict);
            category = Map.copyOf(category); status = Map.copyOf(status); llmStatus = Map.copyOf(llmStatus);
            outcomeReason = Map.copyOf(outcomeReason); llmErrorReason = Map.copyOf(llmErrorReason); sections = Map.copyOf(sections);
        }
        @Override public String toString() { return "ReportLabels[protegidas]"; }
    }
}
