package com.llmscannerseguridad.adapters.in.rest.scan;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.domain.project.Project;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.execution.*;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.aMapWithSize;

class TestExecutionControllerTest {
    private final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    private final ScanRepositoryPort scans = mock(ScanRepositoryPort.class);
    private final TestExecutionRepositoryPort executions = mock(TestExecutionRepositoryPort.class);
    private final UUID project = UUID.randomUUID();
    private final UUID scan = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-10T10:00:00Z");
    private MockMvc mvc;

    @BeforeEach
    void prepare() {
        mvc = MockMvcBuilders.standaloneSetup(new TestExecutionController(
                new ListTestExecutionsUseCase(new GetScanUseCase(projects, scans), executions)))
                .setControllerAdvice(new ScanExceptionHandler()).build();
    }

    @Test
    void invalidIdsHavePrecedenceWithoutPersistence() throws Exception {
        mvc.perform(get("/api/projects/invalid/scans/invalid/executions")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PROJECT_ID"));
        mvc.perform(get(path().replace(scan.toString(), "invalid"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCAN_ID"));
        verifyNoInteractions(projects, scans, executions);
    }

    @Test
    void missingProjectForeignScanAndReadFailureNeverBecomeEmptySuccess() throws Exception {
        when(projects.findById(project)).thenReturn(Optional.empty());
        mvc.perform(get(path())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        presentProject();
        when(scans.findByProjectIdAndId(project, scan)).thenReturn(Optional.empty());
        mvc.perform(get(path())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SCAN_NOT_FOUND"));
        presentScan();
        when(executions.listMetadata(scan)).thenThrow(new IllegalStateException("PRIVATE_BODY_MARKER"));
        mvc.perform(get(path())).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR")).andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE_BODY_MARKER"))));
    }

    @Test
    void emptyHistoricalOrUnpreparedPlanAndFailedResourceUseNoStoreAndExactDto() throws Exception {
        presentProject();
        presentScan();
        when(executions.listMetadata(scan)).thenReturn(List.of());
        mvc.perform(get(path())).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("[]"));
        when(executions.listMetadata(scan)).thenReturn(List.of(new ExecutionMetadata(UUID.randomUUID(), scan,
                "LLM01-INSTRUCTION-001", 1, "Prueba", SecurityCategory.LLM01, ExecutionStatus.FAILED, now, now,
                500, 10L, ExecutionError.HTTP_STATUS_ERROR, false, false)));
        mvc.perform(get(path())).andExpect(status().isOk()).andExpect(jsonPath("$[0]", aMapWithSize(14)))
                .andExpect(jsonPath("$[0].status").value("FAILED")).andExpect(jsonPath("$[0].requestBody").doesNotExist())
                .andExpect(jsonPath("$[0].responseText").doesNotExist()).andExpect(jsonPath("$[0].prompt").doesNotExist());
    }

    private void presentProject() {
        when(projects.findById(project)).thenReturn(Optional.of(new Project(project, "project", null, now, now)));
    }

    private void presentScan() {
        when(scans.findByProjectIdAndId(project, scan)).thenReturn(Optional.of(Scan.pending(scan, project, now)));
    }

    private String path() { return "/api/projects/" + project + "/scans/" + scan + "/executions"; }
}
