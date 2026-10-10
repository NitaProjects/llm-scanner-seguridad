package com.llmscannerseguridad.adapters.in.rest.scan;

import java.time.*;
import java.util.*;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.scoring.*;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.project.Project;
import com.llmscannerseguridad.domain.scan.*;
import com.llmscannerseguridad.domain.scoring.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static com.llmscannerseguridad.application.scoring.ScoringFixtures.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ScoreControllerTest {
    private final ProjectRepositoryPort projects=mock(ProjectRepositoryPort.class);
    private final ScanScoreRepositoryPort scores=mock(ScanScoreRepositoryPort.class);
    private MockMvc mvc;
    private String base() { return "/api/projects/"+PROJECT+"/scans/"+SCAN+"/score"; }
    @BeforeEach void setup() {
        mvc=MockMvcBuilders.standaloneSetup(new ScoreController(new GetScanScoreUseCase(projects,scores)))
                .setControllerAdvice(new EvaluationExceptionHandler()).build();
        when(projects.findById(PROJECT)).thenReturn(Optional.of(new Project(PROJECT,"Proyecto",null,TIME,TIME)));
    }
    private void present(Scan scan,String version,ScanScore score) {
        when(scores.findScoped(PROJECT,SCAN)).thenReturn(Optional.of(new ScanScoreSnapshot(scan,version,EvaluationResult.PROFILE,score)));
    }
    @Test void uuidValidationHasPrecedenceBeforePersistence() throws Exception {
        mvc.perform(get("/api/projects/invalid/scans/invalid/score")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PROJECT_ID")).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(base().replace(SCAN.toString(),"invalid"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SCAN_ID"));
        verifyNoInteractions(scores);verify(projects,never()).findById(any());
    }
    @Test void allAbsenceStatesContainExplicitNullIncludingHistoricalFailed() throws Exception {
        var pending=Scan.pending(SCAN,PROJECT,TIME);
        for(var scan:List.of(pending,pending.start(TIME))) {
            present(scan,ScoringPolicyV1.VERSION,null);
            mvc.perform(get(base())).andExpect(status().isOk()).andExpect(jsonPath("$.availability").value("PENDING"))
                    .andExpect(content().json("{\"availability\":\"PENDING\",\"score\":null}"));
        }
        var failed=pending.fail(ScanFailureReason.SCAN_PROCESSING_FAILED,TIME);
        present(failed,ScoringPolicyV1.VERSION,null);
        mvc.perform(get(base())).andExpect(status().isOk()).andExpect(content().json("{\"availability\":\"UNAVAILABLE\",\"score\":null}"));
        for(var historical:List.of(pending,pending.start(TIME),pending.start(TIME).complete(TIME),failed)) {
            present(historical,null,null);
            mvc.perform(get(base())).andExpect(status().isOk()).andExpect(content().json("{\"availability\":\"NOT_APPLICABLE\",\"score\":null}"));
        }
    }
    @Test void availableZeroAndHundredHaveExactClosedShapeAndUtcMicroseconds() throws Exception {
        for(var verdict:List.of(Verdict.NOT_VULNERABLE,Verdict.VULNERABLE)) {
            var input=inputs(verdict);
            if(verdict==Verdict.VULNERABLE) {
                var evaluations=input.evaluations().stream().map(e->new ScoringInputPort.Evaluation(e.id(),e.scanId(),e.executionId(),e.testId(),e.category(),
                        e.status(),e.verdict(),Severity.CRITICAL,e.confidence(),e.finishedAt())).toList();
                var findings=input.findings().stream().map(f->new ScoringInputPort.Finding(f.id(),f.scanId(),f.executionId(),f.testId(),f.evaluationId(),f.category(),
                        f.verdict(),Severity.CRITICAL,f.confidence(),f.createdAt())).toList();
                input=new ScoringInputPort.Inputs(input.executions(),evaluations,findings);
            }
            var data=input;var score=new CalculateScanScoreUseCase(id->data,Clock.fixed(TIME,ZoneOffset.UTC)).execute(snapshot());
            present(snapshot().scan().complete(score.calculatedAt()),ScoringPolicyV1.VERSION,score);
            mvc.perform(get(base())).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                    .andExpect(jsonPath("$",org.hamcrest.Matchers.aMapWithSize(2))).andExpect(jsonPath("$.availability").value("AVAILABLE"))
                    .andExpect(jsonPath("$.score",org.hamcrest.Matchers.aMapWithSize(11))).andExpect(jsonPath("$.score.globalScore").value(verdict==Verdict.VULNERABLE?100:0))
                    .andExpect(jsonPath("$.score.scoringVersion").value("scoring-v1")).andExpect(jsonPath("$.score.calculatedAt").value(score.calculatedAt().toString()))
                    .andExpect(jsonPath("$.score.severityCounts",org.hamcrest.Matchers.aMapWithSize(4))).andExpect(jsonPath("$.score.categories.length()").value(4))
                    .andExpect(jsonPath("$.score.categories[0]",org.hamcrest.Matchers.aMapWithSize(8))).andExpect(jsonPath("$.score.categories[0].category").value("LLM01"));
        }
        verify(scores,never()).insert(any());verify(scores,never()).lock(any());
    }
    @Test void scopedNotFoundReadErrorsAndCorruptionAreSanitized() throws Exception {
        when(projects.findById(PROJECT)).thenReturn(Optional.empty());
        mvc.perform(get(base())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        verifyNoInteractions(scores);
        when(projects.findById(PROJECT)).thenReturn(Optional.of(new Project(PROJECT,"Proyecto",null,TIME,TIME)));
        mvc.perform(get(base())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SCAN_NOT_FOUND"));
        when(scores.findScoped(PROJECT,SCAN)).thenThrow(new IllegalStateException("SYNTHETIC_PRIVATE_DETAILS"));
        internal();
        reset(scores);
        present(snapshot().scan().complete(TIME),ScoringPolicyV1.VERSION,null);internal();
        present(snapshot().scan(),"unknown-version",null);internal();
        var data=inputs(Verdict.NOT_VULNERABLE);var score=new CalculateScanScoreUseCase(id->data,Clock.fixed(TIME,ZoneOffset.UTC)).execute(snapshot());
        present(snapshot().scan(),ScoringPolicyV1.VERSION,score);internal();
        present(snapshot().scan().complete(score.calculatedAt()),null,score);internal();
    }
    private void internal() throws Exception {
        mvc.perform(get(base())).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SYNTHETIC_PRIVATE_DETAILS"))));
    }
}
