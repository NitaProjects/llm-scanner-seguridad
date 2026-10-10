package com.llmscannerseguridad.application.scoring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.execution.ExecutionStatus;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scoring.ScoringPolicyV1;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

public final class ScoringFixtures {
    public static final Instant TIME = Instant.parse("2026-10-10T12:00:00.123456Z");
    public static final UUID SCAN = UUID.randomUUID(), PROJECT = UUID.randomUUID();
    public static ScanScoreSnapshot snapshot() {
        return new ScanScoreSnapshot(Scan.pending(SCAN,PROJECT,TIME).start(TIME),ScoringPolicyV1.VERSION,EvaluationResult.PROFILE,null);
    }
    public static ScoringInputPort.Inputs inputs(Verdict... verdicts) {
        var executions = new ArrayList<ScoringInputPort.Execution>();
        var evaluations = new ArrayList<ScoringInputPort.Evaluation>();
        var findings = new ArrayList<ScoringInputPort.Finding>();
        String[] codes = {"LLM01-INSTRUCTION-001","LLM02-CANARY-001","LLM08-CONTEXT-001","LLM10-MARKUP-001"};
        for (int i=0;i<4;i++) {
            var category = SecurityCategory.values()[i];
            UUID test=UUID.randomUUID(),execution=UUID.randomUUID(),evaluation=UUID.randomUUID();
            var verdict = verdicts.length == 1 ? verdicts[0] : verdicts[i];
            Severity severity = verdict == Verdict.VULNERABLE ? (i == 0 ? Severity.HIGH : Severity.LOW) : null;
            BigDecimal confidence = new BigDecimal(verdict == Verdict.INCONCLUSIVE ? "0.00" : "0.90");
            executions.add(new ScoringInputPort.Execution(execution,SCAN,test,codes[i],1,category,ExecutionStatus.COMPLETED,TIME.plusSeconds(1)));
            evaluations.add(new ScoringInputPort.Evaluation(evaluation,SCAN,execution,test,category,EvaluationStatus.COMPLETED,
                    verdict,severity,confidence,TIME.plusSeconds(2)));
            if (severity != null) findings.add(new ScoringInputPort.Finding(UUID.randomUUID(),SCAN,execution,test,evaluation,category,
                    Verdict.VULNERABLE,severity,confidence,TIME.plusSeconds(3)));
        }
        return new ScoringInputPort.Inputs(executions,evaluations,findings);
    }
    private ScoringFixtures() { }
}
