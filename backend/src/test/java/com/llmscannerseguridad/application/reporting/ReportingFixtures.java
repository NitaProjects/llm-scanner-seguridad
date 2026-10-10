package com.llmscannerseguridad.application.reporting;

import java.time.*;
import java.util.*;
import com.llmscannerseguridad.adapters.reporting.ReportJson;
import com.llmscannerseguridad.adapters.out.reporting.LocalReportPresentationAdapter;
import com.llmscannerseguridad.application.scoring.*;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.reporting.*;
import static com.llmscannerseguridad.application.scoring.ScoringFixtures.*;

public final class ReportingFixtures {
    public static ReportInputPort.Inputs inputs(Verdict... verdicts) { return inputs(null,verdicts); }
    public static ReportInputPort.Inputs inputs(Severity[] severities, Verdict... verdicts) {
        var data = ScoringFixtures.inputs(verdicts);
        if (severities != null) {
            var old = data;
            var es = new ArrayList<ScoringInputPort.Evaluation>(); var fs = new ArrayList<ScoringInputPort.Finding>();
            for (int i=0;i<4;i++) {
                var e = old.evaluations().get(i); var severity = e.verdict()==Verdict.VULNERABLE?severities[i]:null;
                es.add(new ScoringInputPort.Evaluation(e.id(),e.scanId(),e.executionId(),e.testId(),e.category(),e.status(),e.verdict(),severity,e.confidence(),e.finishedAt()));
                for (var f:old.findings()) if(f.evaluationId().equals(e.id())) fs.add(new ScoringInputPort.Finding(f.id(),f.scanId(),f.executionId(),f.testId(),f.evaluationId(),f.category(),f.verdict(),severity,f.confidence(),f.createdAt()));
            }
            data = new ScoringInputPort.Inputs(old.executions(),es,fs);
        }
        var scoringInputs=data;
        var score=new CalculateScanScoreUseCase(id->scoringInputs,Clock.fixed(TIME,ZoneOffset.UTC)).execute(snapshot());
        var summary=ReportJson.read(ReportJson.write(score),ReportSummary.class);
        var target=UUID.randomUUID();
        var xs=data.executions().stream().map(x->new ReportInputPort.Execution(x.id(),x.scanId(),x.testId(),x.code(),x.revision(),x.category(),x.status(),x.finishedAt(),target,TIME)).toList();
        var fs=data.findings().stream().map(f->{var x=dataExecution(xs,f.executionId());return new ReportInputPort.Finding(f.testId(),new ReportFinding(f.id(),f.evaluationId(),f.scanId(),f.executionId(),x.code(),1,f.category(),f.verdict(),f.severity(),f.confidence(),
                "Observable output issue","Controlled description","Synthetic evidence summary","Review the consumer",f.createdAt()));}).toList();
        var es=data.evaluations().stream().map(e->{var x=dataExecution(xs,e.executionId());var fid=fs.stream().map(ReportInputPort.Finding::value).filter(f->f.evaluationResultId().equals(e.id())).map(ReportFinding::id).findFirst().orElse(null);
            return new ReportInputPort.Evaluation(e.scanId(),e.testId(),new ReportEvaluation(e.id(),e.executionId(),x.code(),1,e.category(),e.status(),e.verdict(),e.confidence(),e.severity(),e.verdict()==Verdict.INCONCLUSIVE?OutcomeReason.CONTEXT_UNAVAILABLE:null,
                LlmStatus.FAILED,LlmError.EVALUATOR_TIMEOUT,true,false,"criterion","1",new ReportEvaluation.Versions("engine-v1","policy-v1","confidence-v1","severity-v1",null,null,null),e.finishedAt(),fid));}).toList();
        return new ReportInputPort.Inputs(snapshot().scan(),"report-v1","scoring-v1","evaluation-engine-v1",summary,xs,es,fs);
    }
    private static ReportInputPort.Execution dataExecution(List<ReportInputPort.Execution> xs,UUID id) { return xs.stream().filter(x->x.id().equals(id)).findFirst().orElseThrow(); }
    public static SecurityReport report(ReportInputPort.Inputs data) {
        return new GenerateReportUseCase(id->data,new LocalReportPresentationAdapter(),Clock.fixed(TIME,ZoneOffset.UTC),UUID::randomUUID).execute(SCAN);
    }
    public static ReportInputPort.Inputs replace(ReportInputPort.Inputs d,List<ReportInputPort.Execution> x,List<ReportInputPort.Evaluation> e,List<ReportInputPort.Finding> f) {
        return new ReportInputPort.Inputs(d.scan(),d.reportVersion(),d.scoringVersion(),d.evaluationProfile(),d.summary(),x,e,f);
    }
    private ReportingFixtures() { }
}
