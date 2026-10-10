package com.llmscannerseguridad.adapters.out.reporting;

import java.nio.charset.StandardCharsets;
import java.util.*;
import com.llmscannerseguridad.adapters.reporting.ReportJson;
import com.llmscannerseguridad.application.reporting.ReportPresentationPort;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.reporting.*;

public final class LocalReportPresentationAdapter implements ReportPresentationPort {
    private final java.util.function.Supplier<String> resource;
    public LocalReportPresentationAdapter() {
        this(() -> {
            try (var stream = LocalReportPresentationAdapter.class.getResourceAsStream("/reporting/report-v1/es.json")) {
                if (stream == null) throw ScanOperationException.internal();
                return new String(stream.readNBytes(32769),StandardCharsets.UTF_8);
            } catch (Exception error) { throw ScanOperationException.internal(); }
        });
    }
    public LocalReportPresentationAdapter(java.util.function.Supplier<String> resource) { this.resource = resource; }
    @Override public Presentation load(String version, String locale) {
        try {
            ReportRules.require(ReportRules.VERSION.equals(version) && ReportRules.LOCALE.equals(locale));
            var json = resource.get(); ReportJson.guard(json.getBytes(StandardCharsets.UTF_8).length,32768);
            var p = ReportJson.read(json,Presentation.class);
            ReportRules.require(version.equals(p.reportVersion()) && locale.equals(p.locale()) && p.interpretation() != null
                    && p.limitations().size() == 9 && p.labels() != null);
            var codes = List.of("IMPLEMENTED_CATEGORIES_ONLY","TARGET_REFERENCE_ONLY","POINT_IN_TIME_TESTS","LLM10_OUTPUT_ONLY",
                    "POSSIBLE_FALSE_RESULTS","INCONCLUSIVE_RESULTS","HEURISTIC_METRICS","EXCLUDED_CAPABILITIES","SYNTHETIC_SCENARIO_SCOPE");
            ReportRules.require(p.limitations().stream().map(SecurityReport.Limitation::code).toList().equals(codes));
            var l = p.labels();
            labels(l.severity(),"LOW","MEDIUM","HIGH","CRITICAL"); labels(l.riskLevel(),"NO_FINDINGS","LOW","MEDIUM","HIGH","CRITICAL");
            labels(l.verdict(),"VULNERABLE","NOT_VULNERABLE","INCONCLUSIVE"); labels(l.category(),"LLM01","LLM02","LLM08","LLM10");
            labels(l.status(),"COMPLETED"); labels(l.llmStatus(),"SUCCEEDED","FAILED","SKIPPED","NOT_REQUESTED");
            labels(l.outcomeReason(),Arrays.stream(com.llmscannerseguridad.domain.evaluation.OutcomeReason.values()).map(Enum::name).toArray(String[]::new));
            labels(l.llmErrorReason(),Arrays.stream(com.llmscannerseguridad.domain.evaluation.LlmError.values()).map(Enum::name).toArray(String[]::new));
            labels(l.sections(),"summary","scope","categories","findings","evaluations","interpretation","limitations");
            return p;
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    private static void labels(Map<String,String> labels, String... keys) {
        ReportRules.require(labels.keySet().equals(Set.of(keys)));
        labels.values().forEach(value -> { ReportRules.text(value,1000); ReportRules.require(!value.contains("://")); });
    }
    public void validate(SecurityReport report) {
        var p = load(report.reportVersion(),report.locale());
        ReportRules.require(p.interpretation().equals(report.interpretation()) && p.limitations().equals(report.limitations()));
    }
}
