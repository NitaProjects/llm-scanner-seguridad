package com.llmscannerseguridad.adapters.in.rest.scan;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.project.Project;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EvaluationControllerTest {
    private final UUID project=UUID.randomUUID(),scan=UUID.randomUUID();
    private final Instant now=Instant.parse("2026-10-10T10:00:00Z");
    private final ProjectRepositoryPort projects=mock(ProjectRepositoryPort.class);
    private final ScanRepositoryPort scans=mock(ScanRepositoryPort.class);
    private final EvaluationRepositoryPort repository=mock(EvaluationRepositoryPort.class);
    private MockMvc mvc;
    @BeforeEach void setup() {
        var getScan=new GetScanUseCase(projects,scans);
        mvc=MockMvcBuilders.standaloneSetup(new EvaluationController(new ListEvaluationsUseCase(getScan,repository),new ListScanFindingsUseCase(getScan,repository)))
                .setControllerAdvice(new EvaluationExceptionHandler()).build();
    }
    private String base() { return "/api/projects/"+project+"/scans/"+scan; }
    private void present() {
        when(projects.findById(project)).thenReturn(Optional.of(new Project(project,"Proyecto",null,now,now)));
        when(scans.findByProjectIdAndId(project,scan)).thenReturn(Optional.of(Scan.pending(scan,project,now)));
    }
    @Test void malformedUuidPrecedenceAppliesBeforeAnyPersistenceForBothGetEndpoints() throws Exception {
        for(String suffix:List.of("evaluations","findings")) {
            mvc.perform(get("/api/projects/invalid/scans/invalid/"+suffix)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PROJECT_ID")).andExpect(header().string("Cache-Control","no-store"));
            mvc.perform(get(base().replace(scan.toString(),"invalid")+"/"+suffix)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_SCAN_ID"));
        }
        verifyNoInteractions(projects,scans,repository);
    }
    @Test void missingProjectForeignScanAndReadFailuresKeepSanitizedErrors() throws Exception {
        mvc.perform(get(base()+"/evaluations")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        present();when(scans.findByProjectIdAndId(project,scan)).thenReturn(Optional.empty());
        mvc.perform(get(base()+"/findings")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SCAN_NOT_FOUND"));
        present();when(repository.listEvaluations(scan)).thenThrow(new IllegalStateException("SYNTHETIC_PRIVATE_DETAILS"));
        mvc.perform(get(base()+"/evaluations")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SYNTHETIC_PRIVATE_DETAILS"))));
    }
    @Test void historicalAndNotYetPreparedPlansHaveExplicitNullProfileOrEmptyItems() throws Exception {
        present();when(repository.listEvaluations(scan)).thenReturn(new EvaluationListing(null,List.of()));
        mvc.perform(get(base()+"/evaluations")).andExpect(status().isOk()).andExpect(content().json("{\"evaluationProfile\":null,\"items\":[]}"));
        when(repository.listEvaluations(scan)).thenReturn(new EvaluationListing(EvaluationResult.PROFILE,List.of()));
        mvc.perform(get(base()+"/evaluations")).andExpect(status().isOk()).andExpect(jsonPath("$.evaluationProfile").value(EvaluationResult.PROFILE));
        when(repository.listFindings(scan)).thenReturn(List.of());mvc.perform(get(base()+"/findings")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }
    @Test void dtoContainsExactlyApprovedFieldsAndLlmErrorIsNotGlobalError() throws Exception {
        present();
        var row=new EvaluationMetadata(UUID.randomUUID(),scan,UUID.randomUUID(),"LLM02-CANARY-001",1,SecurityCategory.LLM02,
                EvaluationStatus.COMPLETED,Verdict.INCONCLUSIVE,new BigDecimal("0.00"),null,LlmStatus.FAILED,LlmError.EVALUATOR_TIMEOUT,
                "OPENAI","gpt-6-luna","LLM02-CRITERIA","1","HYB-07",true,true,OutcomeReason.LLM_COMPONENT_UNAVAILABLE,null,
                "Resumen seguro","Revisar criterio",now,now,null);
        when(repository.listEvaluations(scan)).thenReturn(new EvaluationListing(EvaluationResult.PROFILE,List.of(row)));
        mvc.perform(get(base()+"/evaluations")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0]",org.hamcrest.Matchers.aMapWithSize(26)))
                .andExpect(jsonPath("$.items[0].llmErrorReason").value("EVALUATOR_TIMEOUT"))
                .andExpect(jsonPath("$.items[0].errorReason").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.items[0].findingId").value(org.hamcrest.Matchers.nullValue()));
        verify(repository,never()).findByExecution(any());
    }
}
