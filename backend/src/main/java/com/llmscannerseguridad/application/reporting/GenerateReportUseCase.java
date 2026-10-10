package com.llmscannerseguridad.application.reporting;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.execution.ExecutionStatus;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import com.llmscannerseguridad.domain.reporting.*;
import static com.llmscannerseguridad.domain.reporting.ReportRules.*;

public final class GenerateReportUseCase {
    private final ReportInputPort inputs;
    private final ReportPresentationPort presentation;
    private final Clock clock;
    private final Supplier<UUID> identifiers;
    public GenerateReportUseCase(ReportInputPort inputs, ReportPresentationPort presentation, Clock clock, Supplier<UUID> identifiers) {
        this.inputs = inputs; this.presentation = presentation; this.clock = clock; this.identifiers = identifiers;
    }
    public SecurityReport execute(UUID scanId) {
        try {
            var data = inputs.read(scanId); var scan = data.scan();
            require(scan.id().equals(scanId) && scan.status() == ScanStatus.RUNNING && VERSION.equals(data.reportVersion())
                    && "scoring-v1".equals(data.scoringVersion()) && "evaluation-engine-v1".equals(data.evaluationProfile())
                    && data.summary() != null && data.executions().size() == 4 && data.evaluations().size() == 4 && data.findings().size() <= 4);
            var executions = new HashMap<UUID,ReportInputPort.Execution>(); var tests = new HashSet<UUID>();
            var categories = new HashSet<com.llmscannerseguridad.domain.securitytest.SecurityCategory>();
            var first = data.executions().getFirst();
            for (var e : data.executions()) {
                test(e.code(),e.revision(),e.category()); time(e.finishedAt()); time(e.targetUpdatedAt());
                require(e.id() != null && scanId.equals(e.scanId()) && e.testId() != null && e.targetConfigurationId() != null
                        && executions.putIfAbsent(e.id(),e) == null && tests.add(e.testId()) && categories.add(e.category())
                        && e.status() == ExecutionStatus.COMPLETED && !e.finishedAt().isBefore(scan.startedAt())
                        && e.targetConfigurationId().equals(first.targetConfigurationId()) && e.targetUpdatedAt().equals(first.targetUpdatedAt()));
            }
            var evaluationIds = new HashSet<UUID>(); var evaluatedExecutions = new HashSet<UUID>();
            for (var row : data.evaluations()) {
                var e = row.value(); var x = executions.get(e.testExecutionId());
                require(scanId.equals(row.scanId()) && evaluationIds.add(e.id()) && evaluatedExecutions.add(e.testExecutionId())
                        && x != null && x.testId().equals(row.testId()) && x.category() == e.category()
                        && x.code().equals(e.securityTestCode()) && x.revision() == e.securityTestRevision()
                        && !e.finishedAt().isBefore(x.finishedAt()));
            }
            for (var row : data.findings()) {
                var f = row.value(); var x = executions.get(f.testExecutionId());
                require(x != null && x.testId().equals(row.testId()));
            }
            var texts = presentation.load(VERSION,LOCALE);
            require(VERSION.equals(texts.reportVersion()) && LOCALE.equals(texts.locale()));
            var at = clock.instant().truncatedTo(ChronoUnit.MICROS);
            if (at.isBefore(data.summary().calculatedAt())) at = data.summary().calculatedAt();
            return new SecurityReport(identifiers.get(),scanId,scan.projectId(),at,VERSION,LOCALE,
                    new SecurityReport.Scope(scan.createdAt(),scan.startedAt(),first.targetConfigurationId(),first.targetUpdatedAt()),
                    data.summary(),data.evaluations().stream().map(ReportInputPort.Evaluation::value)
                        .sorted(Comparator.comparing(ReportEvaluation::securityTestCode).thenComparingInt(ReportEvaluation::securityTestRevision)
                            .thenComparing(e -> e.id().toString())).toList(),
                    data.findings().stream().map(ReportInputPort.Finding::value).sorted(ReportFinding.ORDER).toList(),
                    texts.interpretation(),texts.limitations());
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
}
