package com.llmscannerseguridad.application.evaluation;

import java.time.Clock;
import java.util.UUID;
import java.util.function.LongSupplier;
import com.llmscannerseguridad.application.execution.ScanBudget;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.execution.*;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;

public final class EvaluateExecutionUseCase {
    private final EvaluationRepositoryPort repository;
    private final EvaluationPreparationPort preparation;
    private final EvaluatorPort evaluator;
    private final Clock clock;
    private final LongSupplier ticker;
    private final long timeoutMs;

    public EvaluateExecutionUseCase(EvaluationRepositoryPort repository, EvaluationPreparationPort preparation,
            EvaluatorPort evaluator, Clock clock, LongSupplier ticker, long timeoutMs) {
        this.repository = repository; this.preparation = preparation; this.evaluator = evaluator;
        this.clock = clock; this.ticker = ticker; this.timeoutMs = timeoutMs;
    }
    public void execute(TestExecution execution, SecurityTest test, ScanBudget scanBudget) {
        if (execution.status() == ExecutionStatus.FAILED) {
            repository.abortExecution(execution.id(), EvaluationError.EVALUATION_ABORTED);
            return;
        }
        if (execution.status() != ExecutionStatus.COMPLETED) throw ScanOperationException.internal();
        var existing = repository.findByExecution(execution.id()).orElseThrow(ScanOperationException::internal);
        if (existing.status() != EvaluationStatus.PENDING) return;
        // El timeout incluye preparación/reclamación y espera de cuota, además del transporte.
        var budget = new EvaluationCallBudget(scanBudget::remainingNanos, timeoutMs, ticker);
        if (budget.scanExpired()) {
            repository.abortExecution(execution.id(), EvaluationError.SCAN_BUDGET_EXCEEDED);
            throw ScanOperationException.internal();
        }
        EvaluationPreparationPort.Prepared prepared;
        try {
            prepared = preparation.prepare(existing, execution, test);
        } catch (EvaluationPreparationPort.Failure error) {
            repository.abortExecution(execution.id(), error.reason());
            throw ScanOperationException.internal();
        } catch (RuntimeException error) {
            repository.abortExecution(execution.id(), EvaluationError.EVALUATION_INTERNAL_ERROR);
            throw ScanOperationException.internal();
        }
        var claimed = repository.claim(prepared.result(), prepared.insufficiency() == null);
        if (claimed.isEmpty()) return;
        var running = claimed.get();
        var component = running.llm();
        try {
            if (component.attemptCount() == 1) {
                var outcome = evaluator.evaluate(prepared.input(), budget);
                if (budget.scanExpired() || Thread.currentThread().isInterrupted()) {
                    repository.finish(running.fail(clock.instant(), budget.scanExpired()
                            ? EvaluationError.SCAN_BUDGET_EXCEEDED : EvaluationError.EVALUATION_INTERRUPTED));
                    throw ScanOperationException.internal();
                }
                // Defensa adicional contra adapters/fakes que contesten después del timeout.
                if (budget.remainingNanos() == 0) outcome = EvaluatorOutcome.failure(LlmError.EVALUATOR_TIMEOUT);
                outcome = validated(outcome, prepared.input());
                component = new EvaluationResult.LlmComponent(outcome.assessment() == null ? LlmStatus.FAILED : LlmStatus.SUCCEEDED,
                        outcome.errorReason(), 1, outcome.assessment(), outcome.resolvedModel(), outcome.latencyMs(),
                        outcome.inputTokens(), outcome.outputTokens(), component.reservedCostUsd(),
                        component.pricingVersion() == null || component.estimateVersion() == null ? null : outcome.estimatedCostUsd(),
                        component.pricingVersion(), component.estimateVersion(), outcome.providerRequestId());
            }
            var decision = new HybridDecisionPolicy().consolidate(prepared.criteria(), prepared.context(),
                    running.deterministicSignals(), prepared.insufficiency(), component.assessment(),
                    component.status(), running.evidence().redacted());
            String summary = switch (decision.finalVerdict()) {
                case VULNERABLE -> "Se verificó un incumplimiento del criterio en el escenario sintético autorizado.";
                case NOT_VULNERABLE -> "Se verificó la expectativa segura para esta prueba y este escenario.";
                case INCONCLUSIVE -> "La evidencia disponible no permite concluir vulnerabilidad ni seguridad.";
            };
            String recommendation = test.category().name().equals("LLM10")
                    ? "Validar el contrato de salida. La observación se limita a OUTPUT_ONLY y no verifica consumidores posteriores."
                    : "Reforzar la separación de instrucciones y datos y proteger las referencias reservadas del escenario.";
            String evidenceSummary = "Criterio " + prepared.criteria().criteriaId() + "; decisión " + decision.decisionCode()
                    + "; referencias verificadas en la salida observada.";
            repository.finish(running.complete(clock.instant(), decision, component, summary, recommendation, evidenceSummary));
        } catch (RuntimeException error) {
            // Sin reenvío. La compensación de Scan conserva el estado durable si falla el commit.
            throw ScanOperationException.internal();
        }
    }
    private EvaluatorOutcome validated(EvaluatorOutcome outcome, EvaluatorInput input) {
        if (outcome.assessment() == null) return outcome;
        var assessment = outcome.assessment();
        if (input == null || assessment.evidenceReferences().stream().anyMatch(ref ->
                !ref.validFor(input.evidenceText(), input.criteria().criteriaId()))) {
            return EvaluatorOutcome.failure(LlmError.EVALUATOR_INVALID_OUTPUT);
        }
        // La frontera del puerto también protege frente a un adapter alternativo/fake.
        var safe = new LlmAssessment(assessment.verdict(), assessment.confidence(),
                "Valoración del evaluador sujeta a consolidación con evidencia independiente.",
                assessment.suggestedSeverity(), "Revisar la política de seguridad del escenario sintético.",
                assessment.evidenceReferences());
        return new EvaluatorOutcome(safe, null, outcome.resolvedModel(), outcome.latencyMs(),
                outcome.inputTokens(), outcome.outputTokens(), outcome.estimatedCostUsd(), outcome.providerRequestId());
    }
    public boolean allCompleted(UUID scanId) { return repository.allCompleted(scanId); }
}
