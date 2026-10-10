package com.llmscannerseguridad.domain.evaluation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

public record EvaluationResult(UUID id, UUID testExecutionId, UUID scanId, UUID securityTestId,
        SecurityCategory category, EvaluationStatus status, Instant createdAt, Instant startedAt,
        Instant finishedAt, EvaluationError errorReason, HybridDecisionPolicy.Decision decision,
        List<RuleSignal> deterministicSignals, LlmComponent llm, Configuration configuration,
        Evidence evidence, String reasoningSummary, String recommendation, String evidenceSummary) {
    public static final String PROFILE = "evaluation-engine-v1";
    public record Configuration(String criteriaId, String criteriaVersion, String scenarioId, String scenarioVersion,
            String engineVersion, String policyVersion, String confidenceVersion, String severityVersion,
            String promptVersion, String schemaVersion, String llmInputProfile,
            String configurationSnapshot, String configurationHash, String requestedProvider,
            String requestedModel, String samplingParameters) {
        @Override public String toString() { return "EvaluationConfiguration[protegida]"; }
    }
    public record Evidence(String projection, boolean complete, boolean redacted, Integer inputBytes,
            Integer evidenceBytes, String inputHash) {
        @Override public String toString() { return "EvaluationEvidence[protegida]"; }
    }
    public record LlmComponent(LlmStatus status, LlmError errorReason, int attemptCount,
            LlmAssessment assessment, String resolvedModel, Long latencyMs, Long inputTokens,
            Long outputTokens, BigDecimal reservedCostUsd, BigDecimal estimatedCostUsd,
            String pricingVersion, String estimateVersion, String providerRequestId) {
        public LlmComponent {
            Objects.requireNonNull(status);
            if (attemptCount < 0 || attemptCount > 1
                    || status == LlmStatus.SUCCEEDED && (assessment == null || errorReason != null || attemptCount != 1)
                    || status == LlmStatus.FAILED && (errorReason == null || assessment != null || attemptCount != 1)
                    || status == LlmStatus.SKIPPED && (assessment != null || attemptCount != 0)
                    || status == LlmStatus.NOT_REQUESTED && (assessment != null || errorReason != null)
                    || latencyMs != null && latencyMs < 0 || inputTokens != null && inputTokens < 0
                    || outputTokens != null && outputTokens < 0 || reservedCostUsd != null && reservedCostUsd.signum() < 0
                    || estimatedCostUsd != null && estimatedCostUsd.signum() < 0) {
                throw new IllegalArgumentException("Componente LLM no válido.");
            }
        }
        public static LlmComponent pending() {
            return new LlmComponent(LlmStatus.NOT_REQUESTED, null, 0, null, null, null, null, null, null, null, null, null, null);
        }
        @Override public String toString() { return "LlmComponent[status=" + status + ", attemptCount=" + attemptCount + "]"; }
    }
    public EvaluationResult {
        Objects.requireNonNull(id); Objects.requireNonNull(testExecutionId); Objects.requireNonNull(scanId);
        Objects.requireNonNull(securityTestId); Objects.requireNonNull(category); Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt); Objects.requireNonNull(llm);
        deterministicSignals = List.copyOf(deterministicSignals);
        for (var time : new Instant[] {createdAt, startedAt, finishedAt}) {
            if (time != null && time.getNano() % 1000 != 0) throw new IllegalArgumentException("Precisión temporal no válida.");
        }
        boolean valid = switch (status) {
            case PENDING -> startedAt == null && finishedAt == null && errorReason == null && decision == null && llm.attemptCount() == 0;
            case RUNNING -> startedAt != null && finishedAt == null && errorReason == null && decision == null;
            case COMPLETED -> startedAt != null && finishedAt != null && errorReason == null && decision != null
                    && (llm.status() == LlmStatus.SUCCEEDED || llm.status() == LlmStatus.FAILED || llm.status() == LlmStatus.SKIPPED)
                    && (decision.finalVerdict() != Verdict.INCONCLUSIVE || decision.outcomeReason() != null)
                    && (llm.status() == LlmStatus.SUCCEEDED || decision.degraded());
            case FAILED -> finishedAt != null && errorReason != null && decision != null
                    && decision.finalVerdict() == Verdict.INCONCLUSIVE && decision.confidence().signum() == 0 && decision.severity() == null;
        };
        if (!valid || startedAt != null && startedAt.isBefore(createdAt)
                || finishedAt != null && finishedAt.isBefore(startedAt == null ? createdAt : startedAt)
                || decision != null && ((decision.finalVerdict() == Verdict.VULNERABLE) != (decision.severity() != null))
                || decision != null && (decision.confidence().signum() < 0 || decision.confidence().compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("Resultado de evaluación no válido.");
        }
    }

    public static EvaluationResult pending(UUID id, UUID executionId, UUID scanId, UUID testId,
            SecurityCategory category, Instant now, Configuration config) {
        return new EvaluationResult(id, executionId, scanId, testId, category, EvaluationStatus.PENDING,
                latest(now, now), null, null, null, null, List.of(), LlmComponent.pending(), config,
                new Evidence(null, false, false, null, null, null), null, null, null);
    }
    public EvaluationResult start(Instant now, Instant executionFinished, List<RuleSignal> signals,
            Evidence projection, LlmComponent component) {
        require(EvaluationStatus.PENDING);
        return new EvaluationResult(id, testExecutionId, scanId, securityTestId, category, EvaluationStatus.RUNNING,
                createdAt, latest(latest(now, executionFinished), createdAt), null, null, null, signals,
                component, configuration, projection, null, null, null);
    }
    public EvaluationResult complete(Instant now, HybridDecisionPolicy.Decision finalDecision, LlmComponent component,
            String summary, String mitigation, String evidenceText) {
        require(EvaluationStatus.RUNNING);
        return new EvaluationResult(id, testExecutionId, scanId, securityTestId, category, EvaluationStatus.COMPLETED,
                createdAt, startedAt, latest(now, startedAt), null, finalDecision, deterministicSignals,
                component, configuration, evidence, summary, mitigation, evidenceText);
    }
    public EvaluationResult fail(Instant now, EvaluationError reason) {
        if (!status.active()) throw new IllegalStateException("Resultado terminal inmutable.");
        var zero = new BigDecimal("0.00");
        var components = new HybridDecisionPolicy.ConfidenceComponents(zero, zero, zero, new BigDecimal("0.95"), zero);
        var failedDecision = new HybridDecisionPolicy.Decision(Verdict.INCONCLUSIVE, Verdict.INCONCLUSIVE,
                null, components, null, true, false, false, null);
        return new EvaluationResult(id, testExecutionId, scanId, securityTestId, category, EvaluationStatus.FAILED,
                createdAt, startedAt, latest(now, startedAt == null ? createdAt : startedAt), reason, failedDecision,
                deterministicSignals, llm, configuration, evidence, "La evaluación no pudo finalizar técnicamente.",
                "Revisar la configuración y el estado técnico del análisis.", null);
    }
    private void require(EvaluationStatus expected) {
        if (status != expected) throw new IllegalStateException("Transición de evaluación no permitida.");
    }
    public static Instant latest(Instant now, Instant floor) {
        var time = now.truncatedTo(ChronoUnit.MICROS);
        return time.isBefore(floor) ? floor.truncatedTo(ChronoUnit.MICROS) : time;
    }
    @Override public String toString() { return "EvaluationResult[id=" + id + ", status=" + status + "]"; }
}
