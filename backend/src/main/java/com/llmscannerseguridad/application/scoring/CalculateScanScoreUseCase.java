package com.llmscannerseguridad.application.scoring;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.execution.ExecutionStatus;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import com.llmscannerseguridad.domain.scoring.*;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

public final class CalculateScanScoreUseCase {
    private static final Map<SecurityCategory, String> CODES = Map.of(
            SecurityCategory.LLM01, "LLM01-INSTRUCTION-001", SecurityCategory.LLM02, "LLM02-CANARY-001",
            SecurityCategory.LLM08, "LLM08-CONTEXT-001", SecurityCategory.LLM10, "LLM10-MARKUP-001");
    private final ScoringInputPort inputs;
    private final Clock clock;
    private final ScoringPolicyV1 policy = new ScoringPolicyV1();
    public CalculateScanScoreUseCase(ScoringInputPort inputs, Clock clock) { this.inputs = inputs; this.clock = clock; }

    public ScanScore execute(ScanScoreSnapshot snapshot) {
        try {
            snapshot.validate();
            if (snapshot.scan().status() != ScanStatus.RUNNING || !ScoringPolicyV1.VERSION.equals(snapshot.requiredVersion())
                    || snapshot.score() != null) throw ScanOperationException.internal();
            UUID scanId = snapshot.scan().id();
            var data = inputs.read(scanId);
            if (data.executions().size() != 4 || data.evaluations().size() != 4 || data.findings().size() > 4) throw ScanOperationException.internal();
            var executions = new HashMap<UUID, ScoringInputPort.Execution>();
            var categories = EnumSet.noneOf(SecurityCategory.class);
            var tests = new HashSet<UUID>();
            Instant calculated = latest(clock.instant(), snapshot.scan().startedAt());
            for (var row : data.executions()) {
                if (row.id() == null || row.testId() == null || !scanId.equals(row.scanId())
                        || row.category() == null || !CODES.get(row.category()).equals(row.code()) || row.revision() != 1
                        || row.status() != ExecutionStatus.COMPLETED || row.finishedAt() == null
                        || executions.put(row.id(), row) != null || !categories.add(row.category()) || !tests.add(row.testId())) {
                    throw ScanOperationException.internal();
                }
                calculated = latest(calculated, row.finishedAt());
            }
            var evaluations = new HashMap<UUID, ScoringInputPort.Evaluation>();
            var evaluatedExecutions = new HashSet<UUID>();
            for (var row : data.evaluations()) {
                var execution = executions.get(row.executionId());
                if (row.id() == null || execution == null || !scanId.equals(row.scanId()) || !execution.testId().equals(row.testId())
                        || execution.category() != row.category() || row.status() != EvaluationStatus.COMPLETED
                        || row.verdict() == null || row.finishedAt() == null || row.finishedAt().isBefore(execution.finishedAt())
                        || !validConfidence(row.confidence()) || (row.verdict() == Verdict.VULNERABLE) != (row.severity() != null)
                        || row.verdict() == Verdict.INCONCLUSIVE && row.confidence().signum() != 0
                        || evaluations.put(row.id(), row) != null || !evaluatedExecutions.add(row.executionId())) throw ScanOperationException.internal();
                calculated = latest(calculated, row.finishedAt());
            }
            var findings = new HashMap<UUID, ScoringInputPort.Finding>();
            var findingIds = new HashSet<UUID>();
            var findingExecutions = new HashSet<UUID>();
            for (var row : data.findings()) {
                var evaluation = evaluations.get(row.evaluationId());
                if (row.id() == null || !findingIds.add(row.id()) || !findingExecutions.add(row.executionId())
                        || evaluation == null || !scanId.equals(row.scanId()) || !evaluation.executionId().equals(row.executionId())
                        || !evaluation.testId().equals(row.testId()) || evaluation.category() != row.category()
                        || evaluation.verdict() != Verdict.VULNERABLE || row.verdict() != Verdict.VULNERABLE
                        || evaluation.severity() != row.severity() || !validConfidence(row.confidence())
                        || evaluation.confidence().compareTo(row.confidence()) != 0 || row.createdAt() == null
                        || row.createdAt().isBefore(evaluation.finishedAt()) || findings.put(row.evaluationId(), row) != null) throw ScanOperationException.internal();
                calculated = latest(calculated, row.createdAt());
            }
            for (var row : evaluations.values()) {
                if ((row.verdict() == Verdict.VULNERABLE) != findings.containsKey(row.id())) throw ScanOperationException.internal();
            }
            var breakdown = new ArrayList<CategoryScore>();
            for (var category : ScoringPolicyV1.CATEGORIES) {
                var evaluation = evaluations.values().stream().filter(e -> e.category() == category).findFirst().orElseThrow();
                var subset = data.findings().stream().filter(f -> f.category() == category).toList();
                int value = policy.calculate(subset.stream().map(f -> new ScoringPolicyV1.Input(f.severity(), f.confidence())).toList());
                breakdown.add(new CategoryScore(category, value, RiskLevel.fromScore(value), 1, subset.size(),
                        evaluation.verdict() == Verdict.NOT_VULNERABLE ? 1 : 0, evaluation.verdict() == Verdict.INCONCLUSIVE ? 1 : 0, counts(subset)));
            }
            int value = policy.calculate(data.findings().stream().map(f -> new ScoringPolicyV1.Input(f.severity(), f.confidence())).toList());
            return new ScanScore(scanId, ScoringPolicyV1.VERSION, calculated, value, RiskLevel.fromScore(value), 4,
                    data.findings().size(), (int) evaluations.values().stream().filter(e -> e.verdict() == Verdict.NOT_VULNERABLE).count(),
                    (int) evaluations.values().stream().filter(e -> e.verdict() == Verdict.INCONCLUSIVE).count(), counts(data.findings()), breakdown);
        } catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
    private boolean validConfidence(BigDecimal value) { return value != null && value.signum() >= 0 && value.compareTo(BigDecimal.ONE) <= 0; }
    private SeverityCounts counts(List<ScoringInputPort.Finding> findings) {
        int[] counts = new int[4];
        for (var finding : findings) {
            int index = switch (finding.severity()) { case LOW -> 0; case MEDIUM -> 1; case HIGH -> 2; case CRITICAL -> 3; };
            counts[index]++;
        }
        return new SeverityCounts(counts[0], counts[1], counts[2], counts[3]);
    }
    private Instant latest(Instant one, Instant two) {
        if (two.getNano() % 1000 != 0) throw ScanOperationException.internal();
        return (one.isBefore(two) ? two : one).truncatedTo(ChronoUnit.MICROS);
    }
}
