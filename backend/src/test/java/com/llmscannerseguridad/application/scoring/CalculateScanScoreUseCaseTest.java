package com.llmscannerseguridad.application.scoring;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.UnaryOperator;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.execution.ExecutionStatus;
import com.llmscannerseguridad.domain.scoring.*;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import org.junit.jupiter.api.Test;
import static com.llmscannerseguridad.application.scoring.ScoringFixtures.*;
import static org.assertj.core.api.Assertions.*;

class CalculateScanScoreUseCaseTest {
    private ScanScore calculate(ScoringInputPort.Inputs data) {
        return new CalculateScanScoreUseCase(id->data,Clock.fixed(TIME.minusSeconds(10),ZoneOffset.UTC)).execute(snapshot());
    }
    @Test void fourNegativesAndFourInconclusivesRemainDistinctAtZero() {
        for(var verdict:List.of(Verdict.NOT_VULNERABLE,Verdict.INCONCLUSIVE)) {
            var score=calculate(inputs(verdict));
            assertThat(score.globalScore()).isZero();assertThat(score.riskLevel()).isEqualTo(RiskLevel.NO_FINDINGS);
            assertThat(score.evaluationCount()).isEqualTo(4);assertThat(score.findingCount()).isZero();
            assertThat(score.inconclusiveCount()).isEqualTo(verdict==Verdict.INCONCLUSIVE?4:0);
            assertThat(score.notVulnerableCount()).isEqualTo(verdict==Verdict.NOT_VULNERABLE?4:0);
            assertThat(score.categories()).extracting(CategoryScore::category).containsExactly(SecurityCategory.values());
            assertThat(score.calculatedAt()).isEqualTo(TIME.plusSeconds(2));
        }
    }
    @Test void mixedBreakdownCountersAndReconstructionUseOnlyFinalDecisions() {
        var data=inputs(Verdict.VULNERABLE,Verdict.VULNERABLE,Verdict.NOT_VULNERABLE,Verdict.INCONCLUSIVE);
        var result=calculate(data);
        assertThat(result.globalScore()).isEqualTo(76);assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.categories()).extracting(CategoryScore::score).containsExactly(75,15,0,0);
        assertThat(result.severityCounts()).isEqualTo(new SeverityCounts(1,0,1,0));
        assertThat(result.findingCount()).isEqualTo(2);assertThat(result.notVulnerableCount()).isEqualTo(1);assertThat(result.inconclusiveCount()).isEqualTo(1);
        assertThat(result.calculatedAt()).isEqualTo(TIME.plusSeconds(3));
        var executions=new ArrayList<>(data.executions());var evaluations=new ArrayList<>(data.evaluations());var findings=new ArrayList<>(data.findings());
        Collections.reverse(executions);Collections.reverse(evaluations);Collections.reverse(findings);
        assertThat(calculate(new ScoringInputPort.Inputs(executions,evaluations,findings))).isEqualTo(result);
        assertThatThrownBy(()->result.categories().clear()).isInstanceOf(UnsupportedOperationException.class);
        var mixed=calculate(inputs(Verdict.VULNERABLE,Verdict.NOT_VULNERABLE,Verdict.INCONCLUSIVE,Verdict.INCONCLUSIVE));
        assertThat(mixed.globalScore()).isEqualTo(75);assertThat(mixed.inconclusiveCount()).isEqualTo(2);
    }
    @Test void missingExtrasDuplicatesAndForeignRowsAreRejectedWithoutRepair() {
        var data=inputs(Verdict.VULNERABLE);
        rejected(new ScoringInputPort.Inputs(data.executions().subList(0,3),data.evaluations(),data.findings()));
        rejected(new ScoringInputPort.Inputs(data.executions(),data.evaluations().subList(0,3),data.findings()));
        rejected(new ScoringInputPort.Inputs(data.executions(),data.evaluations(),data.findings().subList(0,3)));
        var extra=new ArrayList<>(data.evaluations());extra.add(extra.getFirst());rejected(new ScoringInputPort.Inputs(data.executions(),extra,data.findings()));
        var duplicate=new ArrayList<>(data.executions());duplicate.set(1,duplicate.getFirst());rejected(new ScoringInputPort.Inputs(duplicate,data.evaluations(),data.findings()));
        var duplicateFinding=new ArrayList<>(data.findings());duplicateFinding.set(1,duplicateFinding.getFirst());rejected(new ScoringInputPort.Inputs(data.executions(),data.evaluations(),duplicateFinding));
        var duplicateEvaluation=new ArrayList<>(data.evaluations());duplicateEvaluation.set(1,duplicateEvaluation.getFirst());rejected(new ScoringInputPort.Inputs(data.executions(),duplicateEvaluation,data.findings()));
        var foreign=snapshot();
        assertThatThrownBy(()->new CalculateScanScoreUseCase(id->data,Clock.systemUTC()).execute(new ScanScoreSnapshot(
                com.llmscannerseguridad.domain.scan.Scan.pending(UUID.randomUUID(),PROJECT,TIME).start(TIME),foreign.requiredVersion(),foreign.evaluationProfile(),null)))
                .isInstanceOf(ScanOperationException.class);
    }
    @Test void executionStatesPlansAndEvaluationStatesMustBeComplete() {
        var data=inputs(Verdict.NOT_VULNERABLE);
        for(var state:List.of(ExecutionStatus.PENDING,ExecutionStatus.RUNNING,ExecutionStatus.FAILED)) {
            var row=data.executions().getFirst();var rows=new ArrayList<>(data.executions());
            rows.set(0,new ScoringInputPort.Execution(row.id(),row.scanId(),row.testId(),row.code(),row.revision(),row.category(),state,row.finishedAt()));
            rejected(new ScoringInputPort.Inputs(rows,data.evaluations(),data.findings()));
        }
        for(var state:List.of(EvaluationStatus.PENDING,EvaluationStatus.RUNNING,EvaluationStatus.FAILED)) {
            evaluationMutation(data,e->new ScoringInputPort.Evaluation(e.id(),e.scanId(),e.executionId(),e.testId(),e.category(),state,
                    Verdict.INCONCLUSIVE,null,BigDecimal.ZERO,e.finishedAt()));
        }
        var row=data.executions().getFirst();
        for(String code:List.of("UNKNOWN",row.code())) {
            var rows=new ArrayList<>(data.executions());rows.set(0,new ScoringInputPort.Execution(row.id(),row.scanId(),row.testId(),code,2,row.category(),row.status(),row.finishedAt()));
            rejected(new ScoringInputPort.Inputs(rows,data.evaluations(),data.findings()));
        }
        for(String version:List.of("scoring-v2","")) assertThatThrownBy(()->new CalculateScanScoreUseCase(id->data,Clock.systemUTC())
                .execute(new ScanScoreSnapshot(snapshot().scan(),version,EvaluationResult.PROFILE,null))).isInstanceOf(ScanOperationException.class);
    }
    @Test void findingLinksCategorySeverityConfidenceAndDatesMustMatch() {
        var data=inputs(Verdict.VULNERABLE);
        for(int mutation=0;mutation<9;mutation++) {
            var f=data.findings().getFirst();var rows=new ArrayList<>(data.findings());
            rows.set(0,new ScoringInputPort.Finding(f.id(),mutation==0?UUID.randomUUID():f.scanId(),mutation==1?UUID.randomUUID():f.executionId(),
                    mutation==2?UUID.randomUUID():f.testId(),mutation==3?UUID.randomUUID():f.evaluationId(),mutation==4?SecurityCategory.LLM02:f.category(),
                    mutation==5?Verdict.INCONCLUSIVE:f.verdict(),mutation==6?Severity.LOW:f.severity(),mutation==7?new BigDecimal("0.89"):f.confidence(),
                    mutation==8?TIME:f.createdAt()));
            rejected(new ScoringInputPort.Inputs(data.executions(),data.evaluations(),rows));
        }
        evaluationMutation(data,e->new ScoringInputPort.Evaluation(e.id(),e.scanId(),e.executionId(),e.testId(),e.category(),e.status(),
                Verdict.NOT_VULNERABLE,null,e.confidence(),e.finishedAt()));
        evaluationMutation(data,e->new ScoringInputPort.Evaluation(e.id(),e.scanId(),e.executionId(),e.testId(),e.category(),e.status(),
                e.verdict(),e.severity(),new BigDecimal("1.01"),e.finishedAt()));
    }
    @Test void resultShapeAndCounterInvariantsRejectPartialObjects() {
        var result=calculate(inputs(Verdict.NOT_VULNERABLE));
        assertThatThrownBy(()->new ScanScore(result.scanId(),result.scoringVersion(),result.calculatedAt(),0,result.riskLevel(),4,0,3,0,
                result.severityCounts(),result.categories())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new ScanScore(result.scanId(),result.scoringVersion(),result.calculatedAt(),0,result.riskLevel(),4,0,4,0,
                result.severityCounts(),result.categories().subList(0,3))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new SeverityCounts(-1,0,0,0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new ScanScore(result.scanId(),result.scoringVersion(),result.calculatedAt().plusNanos(1),0,result.riskLevel(),4,0,4,0,
                result.severityCounts(),result.categories())).isInstanceOf(IllegalArgumentException.class);
    }
    private void evaluationMutation(ScoringInputPort.Inputs data,UnaryOperator<ScoringInputPort.Evaluation> mutation) {
        var rows=new ArrayList<>(data.evaluations());rows.set(0,mutation.apply(rows.getFirst()));
        rejected(new ScoringInputPort.Inputs(data.executions(),rows,data.findings()));
    }
    private void rejected(ScoringInputPort.Inputs data) { assertThatThrownBy(()->calculate(data)).isInstanceOf(ScanOperationException.class); }
}
