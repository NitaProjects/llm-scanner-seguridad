package com.llmscannerseguridad.adapters.in.rest.project;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.llmscannerseguridad.application.project.CreateProjectService;
import com.llmscannerseguridad.application.project.GetProjectUseCase;
import com.llmscannerseguridad.application.project.ListProjectsUseCase;
import com.llmscannerseguridad.application.project.ProjectPersistenceException;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.domain.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectControllerTest {

    private final ProjectRepositoryPort repository = mock(ProjectRepositoryPort.class);
    private final UUID id = UUID.fromString("b33bfe85-a166-4bef-bb36-184a1c27e322");
    private final Instant now = Instant.parse("2026-10-09T10:00:00.123456Z");
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var create = new CreateProjectService(repository, Clock.fixed(now, ZoneOffset.UTC), () -> id);
        var controller = new ProjectController(create, new GetProjectUseCase(repository),
                new ListProjectsUseCase(repository), JsonMapper.builder().build());
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ProjectExceptionHandler()).build();
    }

    @Test
    void createsExactlyFiveFieldsWithLocationAndUtcDates() throws Exception {
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Análisis  inicial  \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/projects/" + id))
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Análisis  inicial"))
                .andExpect(jsonPath("$.description").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.createdAt").value(now.toString()))
                .andExpect(jsonPath("$.updatedAt").value(now.toString()));
    }

    @Test
    void rejectsAbsentMalformedNonObjectAndUnknownPropertiesBeforePersistence() throws Exception {
        for (String body : List.of("", " ", "{", "[]", "null", "42", "true", "\"texto\"",
                "{\"name\":\"Proyecto\",\"id\":null}", "{\"name\":\"Proyecto\",\"createdAt\":null}",
                "{\"name\":\"Proyecto\",\"updatedAt\":null}", "{\"name\":\"Proyecto\",\"other\":1}",
                "{\"name\":\"Proyecto\"} {}")) {
            mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void missingNullEmptyAndBlankNamesProduceRequiredFieldErrors() throws Exception {
        for (String body : List.of("{}", "{\"name\":null}", "{\"name\":\"\"}", "{\"name\":\"  \\t\\n\"}")) {
            mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("REQUIRED"));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void wrongTypesAreNeverCoercedToStrings() throws Exception {
        for (String value : List.of("42", "true", "[]", "{}")) {
            mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":" + value + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_TYPE"));
            mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Proyecto\",\"description\":" + value + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("description"))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_TYPE"));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void acceptsSupplementaryUnicodeLimitsAndRejectsOneMoreCodePoint() throws Exception {
        String symbol = "\uD83D\uDD12";
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" " + symbol.repeat(120) + " \",\"description\":\" " + symbol.repeat(2000) + " \"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + symbol.repeat(121) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("TOO_LONG"));
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proyecto\",\"description\":\"" + symbol.repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("description"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("TOO_LONG"));
    }

    @Test
    void strictlyValidatesUuidBeforePersistence() throws Exception {
        for (String value : List.of("no-es-un-uuid", "1-1-1-1-1", "b33bfe85a1664befbb36184a1c27e322",
                "z33bfe85-a166-4bef-bb36-184a1c27e322")) {
            mvc.perform(get("/api/projects/" + value))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PROJECT_ID"))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void getsAnExistingProjectAndReportsAValidMissingUuid() throws Exception {
        when(repository.findById(id)).thenReturn(Optional.of(new Project(id, "Proyecto", null, now, now)));
        mvc.perform(get("/api/projects/" + id.toString().toUpperCase(java.util.Locale.ROOT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.createdAt", endsWith("Z")))
                .andExpect(jsonPath("$.updatedAt").value(now.toString()));
        when(repository.findById(id)).thenReturn(Optional.empty());
        mvc.perform(get("/api/projects/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void returnsEmptyAndOrderedCompleteArrays() throws Exception {
        when(repository.findAllOrdered()).thenReturn(List.of());
        mvc.perform(get("/api/projects")).andExpect(status().isOk()).andExpect(content().json("[]"));
        var low = new Project(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Uno", null, now, now);
        var high = new Project(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), "Dos", null, now, now);
        var older = new Project(id, "Anterior", null, now.minusSeconds(1), now.minusSeconds(1));
        when(repository.findAllOrdered()).thenReturn(List.of(low, high, older));
        mvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(low.id().toString()))
                .andExpect(jsonPath("$[1].id").value(high.id().toString()))
                .andExpect(jsonPath("$[2].id").value(older.id().toString()));
    }

    @Test
    void storageErrorsAlwaysReturnGeneric500WithoutInternalDetails() throws Exception {
        var failure = new ProjectPersistenceException(new IllegalStateException("Detalle interno de almacenamiento"));
        doThrow(failure).when(repository).insert(any());
        when(repository.findById(any())).thenThrow(failure);
        when(repository.findAllOrdered()).thenThrow(failure);
        for (var request : List.of(
                post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Proyecto\"}"),
                get("/api/projects/" + id), get("/api/projects"))) {
            mvc.perform(request).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$", aMapWithSize(3))).andExpect(content().json("""
                    {"code":"INTERNAL_ERROR","message":"No se ha podido completar la operación.","fieldErrors":[]}
                    """));
        }
    }

    @Test
    void rejectsEscapedNullCharactersWithFieldErrorsBeforePersistence() throws Exception {
        var cases = List.of(
                Map.entry("{\"name\":\"NombrePrivado\\u0000Fin\"}", List.of("name")),
                Map.entry("{\"name\":\"Proyecto\",\"description\":\"DescripcionPrivada\\u0000Fin\"}", List.of("description")),
                Map.entry("{\"name\":\"NombrePrivado\\u0000Fin\",\"description\":\"DescripcionPrivada\\u0000Fin\"}",
                        List.of("name", "description")));
        for (var testCase : cases) {
            var result = mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                            .content(testCase.getKey()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", aMapWithSize(3)))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value("Los datos del proyecto no son válidos."))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(testCase.getValue().size())));
            for (int index = 0; index < testCase.getValue().size(); index++) {
                String field = testCase.getValue().get(index);
                String path = "$.fieldErrors[" + index + "]";
                result.andExpect(jsonPath(path, aMapWithSize(3)))
                        .andExpect(jsonPath(path + ".field").value(field))
                        .andExpect(jsonPath(path + ".code").value("INVALID_CHARACTER"))
                        .andExpect(jsonPath(path + ".message").value(field.equals("name")
                                ? "El nombre no admite el carácter U+0000."
                                : "La descripción no admite el carácter U+0000."));
            }
            assertThat(result.andReturn().getResponse().getContentAsString())
                    .doesNotContain("NombrePrivado", "DescripcionPrivada");
            verifyNoInteractions(repository);
        }
    }

    @Test
    void validationResponseContainsOnlyContractFieldsAndNoRejectedValue() throws Exception {
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.message").value("Los datos del proyecto no son válidos."))
                .andExpect(jsonPath("$.fieldErrors[0]", aMapWithSize(3)))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("El nombre es obligatorio."));
    }
}
