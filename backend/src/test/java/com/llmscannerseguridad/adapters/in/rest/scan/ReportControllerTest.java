package com.llmscannerseguridad.adapters.in.rest.scan;

import java.util.*;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.reporting.*;
import com.llmscannerseguridad.domain.project.Project;
import com.llmscannerseguridad.domain.scan.*;
import com.llmscannerseguridad.domain.evaluation.Verdict;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static com.llmscannerseguridad.application.scoring.ScoringFixtures.TIME;
import static com.llmscannerseguridad.application.scoring.ScoringFixtures.SCAN;
import static com.llmscannerseguridad.application.scoring.ScoringFixtures.PROJECT;
import static com.llmscannerseguridad.application.reporting.ReportingFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReportControllerTest {
    private ProjectRepositoryPort projects;private SecurityReportRepositoryPort reports;private MockMvc mvc;
    @BeforeEach void setup() {
        projects=mock(ProjectRepositoryPort.class);reports=mock(SecurityReportRepositoryPort.class);
        mvc=MockMvcBuilders.standaloneSetup(new ReportController(new GetSecurityReportUseCase(projects,reports))).setControllerAdvice(new EvaluationExceptionHandler()).build();
        when(projects.findById(PROJECT)).thenReturn(Optional.of(new Project(PROJECT,"Proyecto",null,TIME,TIME)));
    }
    private String base() { return "/api/projects/"+PROJECT+"/scans/"+SCAN+"/report"; }
    private void present(ReportSnapshot snapshot) { when(reports.findScoped(PROJECT,SCAN)).thenReturn(Optional.of(snapshot)); }
    @Test void availabilityNullsAndAvailableZeroAndHundred() throws Exception {
        var pending=Scan.pending(SCAN,PROJECT,TIME);
        for(var s:List.of(pending,pending.start(TIME))) {
            present(new ReportSnapshot(s,"report-v1","scoring-v1","evaluation-engine-v1",null,null));
            mvc.perform(get(base())).andExpect(status().isOk()).andExpect(content().json("{\"availability\":\"PENDING\",\"report\":null}"));
        }
        var failed=pending.fail(ScanFailureReason.SCAN_PROCESSING_FAILED,TIME);
        present(new ReportSnapshot(failed,"report-v1","scoring-v1","evaluation-engine-v1",null,null));
        mvc.perform(get(base())).andExpect(status().isOk()).andExpect(content().json("{\"availability\":\"UNAVAILABLE\",\"report\":null}"));
        for(var s:List.of(pending,pending.start(TIME),failed,pending.start(TIME).complete(TIME))) {
            present(new ReportSnapshot(s,null,null,null,null,null));
            mvc.perform(get(base())).andExpect(status().isOk()).andExpect(content().json("{\"availability\":\"NOT_APPLICABLE\",\"report\":null}"));
        }
        for(var v:List.of(Verdict.NOT_VULNERABLE,Verdict.INCONCLUSIVE,Verdict.VULNERABLE)) {
            var r=report(inputs(v));present(new ReportSnapshot(pending.start(TIME).complete(r.generatedAt()),"report-v1","scoring-v1","evaluation-engine-v1",r.summary(),r));
            mvc.perform(get(base())).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                    .andExpect(jsonPath("$",org.hamcrest.Matchers.aMapWithSize(2))).andExpect(jsonPath("$.report",org.hamcrest.Matchers.aMapWithSize(12)))
                    .andExpect(jsonPath("$.report.evaluations[0]",org.hamcrest.Matchers.aMapWithSize(19)))
                    .andExpect(jsonPath("$.report.locale").value("es")).andExpect(jsonPath("$.report.reportVersion").value("report-v1"))
                    .andExpect(jsonPath("$.report.summary.globalScore").value(r.summary().globalScore()));
        }
        var critical=report(inputs(new com.llmscannerseguridad.domain.evaluation.Severity[]{com.llmscannerseguridad.domain.evaluation.Severity.CRITICAL,
                com.llmscannerseguridad.domain.evaluation.Severity.HIGH,com.llmscannerseguridad.domain.evaluation.Severity.LOW,com.llmscannerseguridad.domain.evaluation.Severity.MEDIUM},Verdict.VULNERABLE));
        present(new ReportSnapshot(pending.start(TIME).complete(critical.generatedAt()),"report-v1","scoring-v1","evaluation-engine-v1",critical.summary(),critical));
        mvc.perform(get(base())).andExpect(status().isOk()).andExpect(jsonPath("$.report.summary.globalScore").value(100));
        verify(reports,never()).insert(any());verify(reports,never()).lock(any());
    }
    @Test void uuidPrecedenceScopedOwnershipAndSanitizedErrorsNoStore() throws Exception {
        mvc.perform(get(base().replace(PROJECT.toString(),"bad").replace(SCAN.toString(),"bad"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PROJECT_ID")).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(base().replace(SCAN.toString(),"bad"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SCAN_ID"));verifyNoInteractions(reports);verify(projects,never()).findById(any());
        when(projects.findById(PROJECT)).thenReturn(Optional.empty());mvc.perform(get(base())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        when(projects.findById(PROJECT)).thenReturn(Optional.of(new Project(PROJECT,"Proyecto",null,TIME,TIME)));
        mvc.perform(get(base())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SCAN_NOT_FOUND"));
        when(reports.findScoped(PROJECT,SCAN)).thenThrow(new IllegalStateException("SYNTHETIC_PRIVATE_SQL"));internal();reset(reports);
        var s=Scan.pending(SCAN,PROJECT,TIME).start(TIME);
        for(var snapshot:List.of(new ReportSnapshot(s.complete(TIME),"report-v1","scoring-v1","evaluation-engine-v1",null,null),
                new ReportSnapshot(s,"unknown","scoring-v1","evaluation-engine-v1",null,null))) {present(snapshot);internal();}
        var r=report(inputs(Verdict.NOT_VULNERABLE));
        for(var snapshot:List.of(new ReportSnapshot(s,"report-v1","scoring-v1","evaluation-engine-v1",r.summary(),r),
                new ReportSnapshot(s.complete(r.generatedAt()),null,"scoring-v1","evaluation-engine-v1",r.summary(),r),
                new ReportSnapshot(s.complete(r.generatedAt()),"report-v1","scoring-v1","evaluation-engine-v1",null,r))) {present(snapshot);internal();}
    }
    private void internal() throws Exception {
        mvc.perform(get(base())).andExpect(status().isInternalServerError()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR")).andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SYNTHETIC_PRIVATE"))));
    }
}
