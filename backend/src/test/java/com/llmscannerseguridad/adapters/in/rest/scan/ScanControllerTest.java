package com.llmscannerseguridad.adapters.in.rest.scan;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.scan.GetScanUseCase;
import com.llmscannerseguridad.application.scan.ListProjectScansUseCase;
import com.llmscannerseguridad.application.scan.ScanDispatcherPort;
import com.llmscannerseguridad.application.scan.ScanFailureHandlerPort;
import com.llmscannerseguridad.application.scan.ScanLifecyclePort;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.application.scan.ScanRepositoryPort;
import com.llmscannerseguridad.application.scan.StartScanUseCase;
import com.llmscannerseguridad.domain.project.Project;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanFailureReason;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScanControllerTest {
    private final ScanLifecyclePort lifecycle = mock(ScanLifecyclePort.class);
    private final ScanDispatcherPort dispatcher = mock(ScanDispatcherPort.class);
    private final ScanFailureHandlerPort failures = mock(ScanFailureHandlerPort.class);
    private final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    private final ScanRepositoryPort scans = mock(ScanRepositoryPort.class);
    private final UUID project = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-10T08:00:00.123456Z");
    private final Scan pending = Scan.pending(id, project, now);
    private final String path = "/api/projects/" + project + "/scans";
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ScanController(new StartScanUseCase(lifecycle, dispatcher, failures),
                new GetScanUseCase(projects, scans), new ListProjectScansUseCase(projects, scans)))
                .setControllerAdvice(new ScanExceptionHandler()).build();
    }

    @Test
    void acceptsNoBodyWithInitialSevenFieldsAndLocation() throws Exception {
        when(lifecycle.create(project)).thenReturn(pending);
        mvc.perform(post(path)).andExpect(status().isAccepted()).andExpect(header().string("Location", path + "/" + id))
                .andExpect(jsonPath("$", aMapWithSize(7))).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").value(now.toString())).andExpect(jsonPath("$.startedAt").value((Object) null))
                .andExpect(jsonPath("$.finishedAt").value((Object) null)).andExpect(jsonPath("$.errorReason").value((Object) null));
        verify(dispatcher).dispatch(id);
    }

    @Test
    void rejectsEveryNonEmptyBodyBeforeApplicationIncludingNullAndWhitespace() throws Exception {
        for (String body : List.of("{}", "null", " ", "[]", "MARCADOR_ENTRADA", "{\"status\":\"COMPLETED\"}")) {
            var result = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(0))).andReturn();
            assertThat(result.getResponse().getContentAsString()).doesNotContain("MARCADOR_ENTRADA", "COMPLETED");
        }
        verifyNoInteractions(lifecycle, dispatcher, projects, scans);
    }

    @Test
    void rejectsBodyWithoutContentLengthAfterReadingOnlyOneByte() throws Exception {
        var reads = new AtomicInteger();
        var stream = new ServletInputStream() {
            @Override public int read() {
                if (reads.incrementAndGet() > 1) {
                    throw new AssertionError("No debe leerse más de un byte para rechazar el cuerpo.");
                }
                return 'x';
            }
            @Override public boolean isFinished() { return false; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) { }
        };
        mvc.perform(post(path).with(request -> {
            var unknownLength = new MockHttpServletRequest(request.getServletContext(), "POST", path) {
                @Override public int getContentLength() { return -1; }
                @Override public long getContentLengthLong() { return -1; }
                @Override public ServletInputStream getInputStream() { return stream; }
            };
            unknownLength.setContentType(MediaType.APPLICATION_JSON_VALUE);
            unknownLength.addHeader("Transfer-Encoding", "chunked");
            return unknownLength;
        })).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertThat(reads).hasValue(1);
        verifyNoInteractions(lifecycle, dispatcher, projects, scans);
    }

    @Test
    void validatesBothIdsBeforePersistenceWithProjectPrecedence() throws Exception {
        for (String invalid : List.of("invalido", "1-1-1-1-1", "b33bfe85a1664befbb36184a1c27e322")) {
            mvc.perform(get("/api/projects/" + invalid + "/scans/" + invalid)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PROJECT_ID"));
            mvc.perform(get(path + "/" + invalid)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_SCAN_ID"));
            mvc.perform(post("/api/projects/" + invalid + "/scans")).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PROJECT_ID"));
            mvc.perform(get("/api/projects/" + invalid + "/scans")).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(lifecycle, dispatcher, projects, scans);
    }

    @Test
    void pollingObservesConfirmedStatesWithoutMutationOrWorkAndUsesNoStore() throws Exception {
        when(projects.findById(project)).thenReturn(Optional.of(new Project(project, "Proyecto", null, now, now)));
        var running = pending.start(now);
        var completed = running.complete(now);
        var failed = running.fail(ScanFailureReason.SCAN_PROCESSING_FAILED, now);
        when(scans.findByProjectIdAndId(project, id)).thenReturn(Optional.of(pending)).thenReturn(Optional.of(running)).thenReturn(Optional.of(completed)).thenReturn(Optional.of(failed));
        for (String state : List.of("PENDING", "RUNNING", "COMPLETED", "FAILED")) {
            mvc.perform(get(path + "/" + id.toString().toUpperCase(Locale.ROOT)))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$", aMapWithSize(7))).andExpect(jsonPath("$.status").value(state))
                    .andExpect(jsonPath("$.createdAt").value(now.toString()));
        }
        verifyNoInteractions(lifecycle, dispatcher);
        verify(scans, never()).transition(any(), any());
    }

    @Test
    void returnsCompleteOrderedOrEmptyListAndScopedNotFound() throws Exception {
        when(projects.findById(project)).thenReturn(Optional.of(new Project(project, "Proyecto", null, now, now)));
        when(scans.findByProjectIdOrdered(project)).thenReturn(List.of());
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("[]"));
        var newer = Scan.pending(UUID.randomUUID(), project, now.plusSeconds(1));
        when(scans.findByProjectIdOrdered(project)).thenReturn(List.of(newer, pending));
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(newer.id().toString())).andExpect(jsonPath("$[1].id").value(id.toString()));
        mvc.perform(get(path + "/" + UUID.randomUUID())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCAN_NOT_FOUND"));
        when(projects.findById(project)).thenReturn(Optional.empty());
        mvc.perform(get(path + "/" + id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @Test
    void mapsPreconditionConflictAndUnavailableErrorsWithoutLocation() throws Exception {
        var cases = List.of(
                java.util.Map.entry(ScanOperationException.Reason.PROJECT_NOT_FOUND, 404),
                java.util.Map.entry(ScanOperationException.Reason.TARGET_NOT_CONFIGURED, 404),
                java.util.Map.entry(ScanOperationException.Reason.SCAN_ALREADY_ACTIVE, 409),
                java.util.Map.entry(ScanOperationException.Reason.SCAN_EXECUTION_UNAVAILABLE, 503),
                java.util.Map.entry(ScanOperationException.Reason.INTERNAL_ERROR, 500));
        for (var entry : cases) {
            doThrow(new ScanOperationException(entry.getKey())).when(lifecycle).create(project);
            mvc.perform(post(path)).andExpect(status().is(entry.getValue())).andExpect(header().doesNotExist("Location"))
                    .andExpect(jsonPath("$", aMapWithSize(3))).andExpect(jsonPath("$.code").value(entry.getKey().name()))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
        }
        verifyNoInteractions(dispatcher);
    }

    @Test
    void dispatchAndCompensationFailuresKeepLocationAndSanitizeAllTechnicalDetails() throws Exception {
        when(lifecycle.create(project)).thenReturn(pending);
        doThrow(new IllegalStateException("MARCADOR_INTERNO_DISPATCH")).when(dispatcher).dispatch(id);
        when(lifecycle.fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_DISPATCH_FAILED)).thenReturn(true);
        mvc.perform(post(path)).andExpect(status().isServiceUnavailable()).andExpect(header().string("Location", path + "/" + id))
                .andExpect(jsonPath("$.code").value("SCAN_DISPATCH_FAILED"));
        when(lifecycle.fail(id, ScanStatus.PENDING, ScanFailureReason.SCAN_DISPATCH_FAILED)).thenThrow(new IllegalStateException("MARCADOR_SQL"));
        var response = mvc.perform(post(path)).andExpect(status().isInternalServerError())
                .andExpect(header().string("Location", path + "/" + id)).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andReturn();
        assertThat(response.getResponse().getContentAsString()).doesNotContain("MARCADOR", "Exception", "SQL");
        verify(failures).persistenceFailed(id);
    }

    @Test
    void readFailuresRemain500InsteadOfMissingOrEmptyWithoutInternalData() throws Exception {
        when(projects.findById(project)).thenReturn(Optional.of(new Project(project, "Proyecto", null, now, now)));
        when(scans.findByProjectIdAndId(project, id)).thenThrow(new IllegalStateException("MARCADOR_DB_CONEXION"));
        when(scans.findByProjectIdOrdered(project)).thenThrow(new IllegalStateException("MARCADOR_DB_CONEXION"));
        for (String url : List.of(path, path + "/" + id)) {
            var result = mvc.perform(get(url)).andExpect(status().isInternalServerError()).andExpect(content().json("""
                    {"code":"INTERNAL_ERROR","message":"No se ha podido completar la operación.","fieldErrors":[]}
                    """)).andReturn();
            assertThat(result.getResponse().getContentAsString()).doesNotContain("MARCADOR", "Exception");
        }
    }
}
