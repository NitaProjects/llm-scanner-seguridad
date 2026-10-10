package com.llmscannerseguridad.application.evaluation;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import com.llmscannerseguridad.adapters.out.evaluation.EvaluationFixtures;
import com.llmscannerseguridad.application.execution.ScanBudget;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.execution.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvaluateExecutionUseCaseTest {
    private final Instant now=Instant.parse("2026-10-10T10:00:00Z");
    private final Clock clock=Clock.fixed(now,ZoneOffset.UTC);
    private final AtomicLong time=new AtomicLong();
    private final EvaluationRepositoryPort repository=mock(EvaluationRepositoryPort.class);
    private final EvaluationPreparationPort preparation=mock(EvaluationPreparationPort.class);
    private final EvaluatorPort evaluator=mock(EvaluatorPort.class);
    private final com.llmscannerseguridad.domain.securitytest.SecurityTest test=EvaluationFixtures.test();
    private final TestExecution execution=TestExecution.pending(UUID.randomUUID(),UUID.randomUUID(),test.id(),UUID.randomUUID(),now)
            .start(now,now).finish(now,200,1L,null,"{}","SYNTHETIC_CANARY_7",false,false);
    private final EvaluationResult pending=EvaluationResult.pending(UUID.randomUUID(),execution.id(),execution.scanId(),test.id(),test.category(),now,null);
    private EvaluateExecutionUseCase useCase() { return new EvaluateExecutionUseCase(repository,preparation,evaluator,clock,time::get,20000); }
    private ScanBudget budget() { return new ScanBudget(now,300000,clock,time::get); }
    private void prepare() {
        var input=EvaluationFixtures.input(test,execution.responseText());
        var running=pending.start(now,now,input.deterministicSignals(),new EvaluationResult.Evidence(null,true,false,100,18,null),
                new EvaluationResult.LlmComponent(LlmStatus.NOT_REQUESTED,null,1,null,null,null,null,null,null,null,null,null,null));
        when(repository.findByExecution(execution.id())).thenReturn(Optional.of(pending));
        when(preparation.prepare(pending,execution,test)).thenReturn(new EvaluationPreparationPort.Prepared(running,input.criteria(),input.trustedSyntheticContext(),input,null));
        when(repository.claim(running,true)).thenReturn(Optional.of(running));
    }
    @Test void lateIndividualResponseUsesStrongFallbackButGlobalDeadlineFailsTechnically() {
        for(long elapsed:List.of(20000000001L,300000000001L)) {
            reset(repository,preparation,evaluator);time.set(0);prepare();
            when(evaluator.evaluate(any(),any())).thenAnswer(call->{time.addAndGet(elapsed);
                return EvaluatorOutcome.failure(LlmError.EVALUATOR_TIMEOUT);});
            if(elapsed>300000000000L) assertThatThrownBy(()->useCase().execute(execution,test,budget())).isInstanceOf(ScanOperationException.class);
            else useCase().execute(execution,test,budget());
            var result=ArgumentCaptor.forClass(EvaluationResult.class);verify(repository).finish(result.capture());
            assertThat(result.getValue().status()).isEqualTo(elapsed>300000000000L?EvaluationStatus.FAILED:EvaluationStatus.COMPLETED);
            assertThat(result.getValue().errorReason()).isEqualTo(elapsed>300000000000L?EvaluationError.SCAN_BUDGET_EXCEEDED:null);
            assertThat(result.getValue().decision().finalVerdict()).isEqualTo(elapsed>300000000000L?Verdict.INCONCLUSIVE:Verdict.VULNERABLE);
        }
    }
    @Test void failedPreparationClosesOnlyWithGlobalReasonAndNeverCallsLlm() {
        for(var reason:List.of(EvaluationError.CRITERIA_UNAVAILABLE,EvaluationError.EVALUATION_INTERNAL_ERROR)) {
            reset(repository,preparation,evaluator);prepare();
            when(preparation.prepare(pending,execution,test)).thenThrow(new EvaluationPreparationPort.Failure(reason));
            assertThatThrownBy(()->useCase().execute(execution,test,budget())).isInstanceOf(ScanOperationException.class);
            verify(repository).abortExecution(execution.id(),reason);verifyNoInteractions(evaluator);
        }
    }
    @Test void expiredBudgetDoesNotPrepareOrReserveAnAttempt() {
        when(repository.findByExecution(execution.id())).thenReturn(Optional.of(pending));
        var budget=budget();time.set(300000000001L);
        assertThatThrownBy(()->useCase().execute(execution,test,budget)).isInstanceOf(ScanOperationException.class);
        verify(repository).abortExecution(execution.id(),EvaluationError.SCAN_BUDGET_EXCEEDED);
        verifyNoInteractions(preparation,evaluator);
    }
    @Test void failedTargetClosesPendingEvaluationWithoutSemanticCall() {
        var failed=TestExecution.pending(execution.id(),execution.scanId(),test.id(),execution.targetConfigurationId(),now)
                .abort(now,now,ExecutionError.EXECUTION_ABORTED);
        useCase().execute(failed,test,budget());
        verify(repository).abortExecution(failed.id(),EvaluationError.EVALUATION_ABORTED);verifyNoInteractions(preparation,evaluator);
    }
    @Test void arbitraryPortNarrativeIsDiscardedAndInventedReferencesBecomeComponentFailure() {
        for(boolean invalid:List.of(false,true)) {
            reset(repository,preparation,evaluator);prepare();
            var input=EvaluationFixtures.input(test,execution.responseText());
            var assessment=new LlmAssessment(Verdict.VULNERABLE,new java.math.BigDecimal("0.9"),"SYNTHETIC_PRIVATE_CHAIN_OF_THOUGHT",
                    Severity.CRITICAL,"<script>SYNTHETIC_PRIVATE_KEY</script>",List.of(new EvidenceReference("response",0,4,invalid?"INVENTED":input.criteria().criteriaId())));
            when(evaluator.evaluate(any(),any())).thenReturn(new EvaluatorOutcome(assessment,null,null,null,null,null,null,null));
            useCase().execute(execution,test,budget());var result=ArgumentCaptor.forClass(EvaluationResult.class);verify(repository).finish(result.capture());
            assertThat(result.getValue().llm().errorReason()).isEqualTo(invalid?LlmError.EVALUATOR_INVALID_OUTPUT:null);
            assertThat(result.getValue().decision().finalVerdict()).isEqualTo(Verdict.VULNERABLE);
            assertThat(result.getValue().reasoningSummary()).doesNotContain("SYNTHETIC_PRIVATE");
            if(!invalid) assertThat(result.getValue().llm().assessment().reasoningSummary()+result.getValue().llm().assessment().recommendation())
                    .doesNotContain("SYNTHETIC_PRIVATE","<script>");
        }
    }
}
