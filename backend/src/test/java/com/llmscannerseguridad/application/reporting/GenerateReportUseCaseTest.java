package com.llmscannerseguridad.application.reporting;

import java.time.*;
import java.util.*;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.reporting.*;
import com.llmscannerseguridad.adapters.out.reporting.LocalReportPresentationAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static com.llmscannerseguridad.application.reporting.ReportingFixtures.*;
import static com.llmscannerseguridad.application.scoring.ScoringFixtures.*;
import static org.assertj.core.api.Assertions.*;

class GenerateReportUseCaseTest {
    @ParameterizedTest @EnumSource(Verdict.class) void copiesAllResultsCountsAndLiteralEnglishTexts(Verdict verdict) {
        var d=ReportingFixtures.inputs(verdict); var r=report(d);
        assertThat(r.summary()).isSameAs(d.summary()); assertThat(r.locale()).isEqualTo("es"); assertThat(r.reportVersion()).isEqualTo("report-v1");
        assertThat(r.evaluations()).hasSize(4); assertThat(r.summary().categories()).hasSize(4); assertThat(r.limitations()).hasSize(9);
        assertThat(r.summary().globalScore()).isEqualTo(verdict==Verdict.VULNERABLE?79:0);
        assertThat(r.summary().inconclusiveCount()).isEqualTo(verdict==Verdict.INCONCLUSIVE?4:0);
        assertThat(r.summary().notVulnerableCount()).isEqualTo(verdict==Verdict.NOT_VULNERABLE?4:0);
        assertThat(r.findings()).containsExactlyElementsOf(d.findings().stream().map(ReportInputPort.Finding::value).sorted(ReportFinding.ORDER).toList());
        assertThat(r.findings()).allSatisfy(f->{assertThat(f.title()).isEqualTo("Observable output issue");assertThat(f.description()).isEqualTo("Controlled description");
            assertThat(f.evidenceSummary()).isEqualTo("Synthetic evidence summary");assertThat(f.recommendation()).isEqualTo("Review the consumer");});
        assertThat(r.interpretation().scoreMeaning()).contains("No se detectaron Findings en las pruebas completadas cuando el score es 0", "no demuestra que la aplicación sea segura");
        assertThat(r.toString()).doesNotContain("Observable","consumer"); assertThat(r.generatedAt()).isEqualTo(d.summary().calculatedAt());
    }
    @ParameterizedTest @EnumSource(Severity.class) void severityAndScoreAreCopiedNotReinterpreted(Severity severity) {
        var d=ReportingFixtures.inputs(new Severity[]{severity,severity,severity,severity},Verdict.VULNERABLE);var r=report(d);
        assertThat(r.summary()).isEqualTo(d.summary()); assertThat(r.findings()).allSatisfy(f->assertThat(f.severity()).isEqualTo(severity));
        if(severity==Severity.CRITICAL) assertThat(r.summary().globalScore()).isEqualTo(100);
    }
    @Test void mixedFallbackAndOrderingAreStableWithDifferentInputOrderAndClock() {
        var d=ReportingFixtures.inputs(Verdict.VULNERABLE,Verdict.VULNERABLE,Verdict.NOT_VULNERABLE,Verdict.INCONCLUSIVE);
        var r=report(d);assertThat(r.summary().globalScore()).isEqualTo(76);
        assertThat(r.evaluations()).allSatisfy(e->{assertThat(e.llmStatus()).isEqualTo(LlmStatus.FAILED);assertThat(e.llmErrorReason()).isEqualTo(LlmError.EVALUATOR_TIMEOUT);assertThat(e.degraded()).isTrue();});
        var xs=new ArrayList<>(d.executions());var es=new ArrayList<>(d.evaluations());var fs=new ArrayList<>(d.findings());
        Collections.reverse(xs);Collections.reverse(es);Collections.reverse(fs);
        var reversed=replace(d,xs,es,fs);var later=new GenerateReportUseCase(id->reversed,new LocalReportPresentationAdapter(),Clock.fixed(TIME.plusSeconds(20),ZoneOffset.UTC),UUID::randomUUID).execute(SCAN);
        assertThat(r.sameContent(later)).isTrue(); assertThat(later.generatedAt()).isAfter(r.generatedAt());assertThat(later.reportId()).isNotEqualTo(r.reportId());
    }
    @Test void rejectsPartialExtraDuplicateForeignAndDivergentTargetRows() {
        var d=ReportingFixtures.inputs(Verdict.VULNERABLE);
        var invalid=new ArrayList<ReportInputPort.Inputs>();
        invalid.add(replace(d,d.executions().subList(0,3),d.evaluations(),d.findings()));
        invalid.add(replace(d,d.executions(),d.evaluations().subList(0,3),d.findings()));
        invalid.add(replace(d,d.executions(),d.evaluations(),d.findings().subList(0,3)));
        var xs=new ArrayList<>(d.executions());xs.set(1,xs.getFirst());invalid.add(replace(d,xs,d.evaluations(),d.findings()));
        var es=new ArrayList<>(d.evaluations());es.set(1,es.getFirst());invalid.add(replace(d,d.executions(),es,d.findings()));
        var fs=new ArrayList<>(d.findings());fs.add(fs.getFirst());invalid.add(replace(d,d.executions(),d.evaluations(),fs));
        var x=d.executions().getFirst();xs=new ArrayList<>(d.executions());
        xs.set(0,new ReportInputPort.Execution(x.id(),x.scanId(),x.testId(),x.code(),1,x.category(),x.status(),x.finishedAt(),UUID.randomUUID(),x.targetUpdatedAt()));
        invalid.add(replace(d,xs,d.evaluations(),d.findings()));
        es=new ArrayList<>(d.evaluations());var e=es.getFirst();es.set(0,new ReportInputPort.Evaluation(UUID.randomUUID(),e.testId(),e.value()));invalid.add(replace(d,d.executions(),es,d.findings()));
        fs=new ArrayList<>(d.findings());var f=fs.getFirst();fs.set(0,new ReportInputPort.Finding(UUID.randomUUID(),f.value()));invalid.add(replace(d,d.executions(),d.evaluations(),fs));
        invalid.forEach(i->assertThatThrownBy(()->report(i)).isInstanceOf(ScanOperationException.class));
    }
    @Test void presentationFailureIsTechnicalWithoutChangingSources() {
        var d=ReportingFixtures.inputs(Verdict.INCONCLUSIVE);
        assertThatThrownBy(()->new GenerateReportUseCase(id->d,(v,l)->{throw new IllegalStateException("SYNTHETIC_PRIVATE");},Clock.systemUTC(),UUID::randomUUID).execute(SCAN))
                .isInstanceOf(ScanOperationException.class).hasMessageNotContaining("SYNTHETIC_PRIVATE");
        assertThat(d.summary().inconclusiveCount()).isEqualTo(4);
    }
    @Test void oneLowHasOneOriginalRecommendationAndThreeNegatives() {
        var d=ReportingFixtures.inputs(new Severity[]{Severity.LOW,null,null,null},Verdict.VULNERABLE,Verdict.NOT_VULNERABLE,Verdict.NOT_VULNERABLE,Verdict.NOT_VULNERABLE);
        var r=report(d);assertThat(r.summary().globalScore()).isEqualTo(15);assertThat(r.findings()).hasSize(1);
        assertThat(r.summary().notVulnerableCount()).isEqualTo(3);assertThat(r.findings().getFirst().recommendation()).isEqualTo("Review the consumer");
    }
}
