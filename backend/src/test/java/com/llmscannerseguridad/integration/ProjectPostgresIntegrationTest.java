package com.llmscannerseguridad.integration;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.llmscannerseguridad.LlmScannerSeguridadApplication;
import com.llmscannerseguridad.adapters.in.rest.project.ProjectController;
import com.llmscannerseguridad.adapters.in.rest.project.ProjectExceptionHandler;
import com.llmscannerseguridad.adapters.in.rest.project.ProjectResponse;
import com.llmscannerseguridad.application.project.CreateProjectCommand;
import com.llmscannerseguridad.application.project.CreateProjectUseCase;
import com.llmscannerseguridad.application.project.GetProjectUseCase;
import com.llmscannerseguridad.application.project.ListProjectsUseCase;
import com.llmscannerseguridad.application.project.ProjectPersistenceException;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.domain.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PM-01 a PM-07: requiere PostgreSQL dedicado y autorización para ejecutar tests/migraciones.
@EnabledIfEnvironmentVariable(named = "PROJECT_TEST_DB_URL", matches = "jdbc:postgresql:.+")
class ProjectPostgresIntegrationTest {

    @Test
    void adapterPersistsDuplicatesNullDescriptionDatesAndCanonicalUuidOrder() throws Exception {
        try (var database = new TestDatabase()) {
            var context = database.open();
            var repository = context.getBean(ProjectRepositoryPort.class);
            var create = context.getBean(CreateProjectUseCase.class);
            var first = create.execute(new CreateProjectCommand(" Proyecto repetido ", " "));
            var second = create.execute(new CreateProjectCommand("Proyecto repetido", "Descripción"));
            assertThat(first.id()).isNotEqualTo(second.id());
            assertThat(repository.findById(first.id())).contains(first);
            assertThat(repository.findById(second.id())).contains(second);
            assertThat(first.description()).isNull();
            assertThat(first.updatedAt()).isEqualTo(first.createdAt());
            assertThat(first.createdAt().getNano() % 1000).isZero();

            Instant tie = Instant.parse("2100-01-01T00:00:00.123456Z");
            var low = new Project(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Empate", null, tie, tie);
            var high = new Project(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), "Empate", null, tie, tie);
            var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            transaction.executeWithoutResult(status -> {
                repository.insert(high);
                repository.insert(low);
            });
            var projects = repository.findAllOrdered();
            assertThat(projects).hasSize(4);
            assertThat(projects.subList(0, 2)).containsExactly(low, high);
            assertThat(projects).contains(first, second);
            assertThat(repository.findById(first.id())).contains(first);
        }
    }

    @Test
    void restApplicationAndPostgresPreserveUnicodeAndValidationContract() throws Exception {
        try (var database = new TestDatabase()) {
            var context = database.open();
            var mvc = mvc(context);
            var mapper = context.getBean(JsonMapper.class);
            mvc.perform(get("/api/projects")).andExpect(status().isOk()).andExpect(content().json("[]"));
            String symbol = "\uD83D\uDD12";
            String body = "{\"name\":\" " + symbol.repeat(120) + " \",\"description\":\" " + symbol.repeat(2000) + " \"}";
            var response = mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$", aMapWithSize(5))).andReturn();
            var created = mapper.readValue(response.getResponse().getContentAsString(StandardCharsets.UTF_8), ProjectResponse.class);
            assertThat(created.name()).isEqualTo(symbol.repeat(120));
            assertThat(created.description()).isEqualTo(symbol.repeat(2000));
            assertThat(created.updatedAt()).isEqualTo(created.createdAt()).endsWith("Z");
            String location = response.getResponse().getHeader("Location");
            assertThat(location).isEqualTo("/api/projects/" + created.id());
            var fetched = mvc.perform(get(location)).andExpect(status().isOk()).andReturn();
            assertThat(mapper.readValue(fetched.getResponse().getContentAsString(StandardCharsets.UTF_8), ProjectResponse.class)).isEqualTo(created);

            var repeated = mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated()).andReturn();
            var duplicate = mapper.readValue(repeated.getResponse().getContentAsString(StandardCharsets.UTF_8), ProjectResponse.class);
            assertThat(duplicate.id()).isNotEqualTo(created.id());
            mvc.perform(get("/api/projects")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));

            mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + symbol.repeat(121) + "\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].code").value("TOO_LONG"));
            mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Proyecto\",\"description\":\"" + symbol.repeat(2001) + "\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("description"));
            mvc.perform(get("/api/projects/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
            mvc.perform(get("/api/projects/1-1-1-1-1"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PROJECT_ID"));
            assertThat(context.getBean(ProjectRepositoryPort.class).findAllOrdered()).hasSize(2);
        }
    }

    @Test
    void escapedNullCharactersAreRejectedWithoutInsertingProjects() throws Exception {
        try (var database = new TestDatabase()) {
            var context = database.open();
            var mvc = mvc(context);
            var jdbc = context.getBean(JdbcTemplate.class);
            var cases = List.of(
                    Map.entry("{\"name\":\"NombrePrivado\\u0000Fin\"}", List.of("name")),
                    Map.entry("{\"name\":\"Proyecto\",\"description\":\"DescripcionPrivada\\u0000Fin\"}", List.of("description")),
                    Map.entry("{\"name\":\"NombrePrivado\\u0000Fin\",\"description\":\"DescripcionPrivada\\u0000Fin\"}",
                            List.of("name", "description")));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM projects", Long.class)).isZero();
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
                assertThat(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .doesNotContain("NombrePrivado", "DescripcionPrivada");
                assertThat(jdbc.queryForObject("SELECT count(*) FROM projects", Long.class)).isZero();
            }
        }
    }

    @Test
    void restListsTemporalTiesByCanonicalUuidWithoutChangingDates() throws Exception {
        try (var database = new TestDatabase()) {
            var context = database.open();
            var repository = context.getBean(ProjectRepositoryPort.class);
            Instant now = Instant.parse("2026-10-09T10:00:00.123456Z");
            var low = new Project(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Uno", null, now, now);
            var high = new Project(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), "Dos", null, now, now);
            var older = new Project(UUID.randomUUID(), "Anterior", null, now.minusSeconds(1), now.minusSeconds(1));
            new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(status -> {
                repository.insert(older);
                repository.insert(high);
                repository.insert(low);
            });
            mvc(context).perform(get("/api/projects"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id").value(low.id().toString()))
                    .andExpect(jsonPath("$[1].id").value(high.id().toString()))
                    .andExpect(jsonPath("$[2].id").value(older.id().toString()));
            assertThat(repository.findById(low.id())).contains(low);
            assertThat(repository.findById(high.id())).contains(high);
            assertThat(repository.findById(older.id())).contains(older);
        }
    }

    @Test
    void deferredCommitFailureReturns500AndRollsBackTheWholeInsertion() throws Exception {
        try (var database = new TestDatabase()) {
            var context = database.open();
            var jdbc = context.getBean(JdbcTemplate.class);
            // Fixture exclusiva del esquema aislado: falla después del flush, durante el commit.
            jdbc.execute("""
                    CREATE FUNCTION reject_project_commit() RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN RAISE EXCEPTION 'Fallo simulado de commit'; END;
                    $$
                    """);
            jdbc.execute("""
                    CREATE CONSTRAINT TRIGGER reject_project_commit AFTER INSERT ON projects
                    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION reject_project_commit()
                    """);
            assertThatThrownBy(() -> context.getBean(CreateProjectUseCase.class)
                    .execute(new CreateProjectCommand("Proyecto", null)))
                    .isInstanceOf(ProjectPersistenceException.class);
            mvc(context).perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Proyecto\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$", aMapWithSize(3)))
                    .andExpect(content().json("""
                            {"code":"INTERNAL_ERROR","message":"No se ha podido completar la operación.","fieldErrors":[]}
                            """));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM projects", Long.class)).isZero();
        }
    }

    @Test
    void storageReadFailuresNeverBecomeNotFoundOrEmptyList() throws Exception {
        try (var database = new TestDatabase()) {
            var context = database.open();
            context.getBean(JdbcTemplate.class).execute("ALTER TABLE projects RENAME TO projects_unavailable");
            var mvc = mvc(context);
            for (String path : List.of("/api/projects", "/api/projects/" + UUID.randomUUID())) {
                mvc.perform(get(path)).andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$", aMapWithSize(3)))
                        .andExpect(content().json("""
                                {"code":"INTERNAL_ERROR","message":"No se ha podido completar la operación.","fieldErrors":[]}
                                """));
            }
        }
    }

    @Test
    void aNewApplicationInstanceRecoversAllFiveValuesFromTheSameDatabase() throws Exception {
        try (var database = new TestDatabase()) {
            var first = database.open();
            var project = first.getBean(CreateProjectUseCase.class)
                    .execute(new CreateProjectCommand("Persistencia tras reinicio", "Descripción conservada"));
            database.closeApplication(first);

            var second = database.open();
            assertThat(second.getBean(GetProjectUseCase.class).execute(project.id())).isEqualTo(project);
            assertThat(second.getBean(ListProjectsUseCase.class).execute()).containsExactly(project);
            mvc(second).perform(get("/api/projects/" + project.id())).andExpect(status().isOk())
                    .andExpect(jsonPath("$", aMapWithSize(5)))
                    .andExpect(jsonPath("$.id").value(project.id().toString()))
                    .andExpect(jsonPath("$.name").value(project.name()))
                    .andExpect(jsonPath("$.description").value(project.description()))
                    .andExpect(jsonPath("$.createdAt").value(project.createdAt().toString()))
                    .andExpect(jsonPath("$.updatedAt").value(project.updatedAt().toString()));
        }
    }

    private MockMvc mvc(ConfigurableApplicationContext context) {
        return MockMvcBuilders.standaloneSetup(context.getBean(ProjectController.class))
                .setControllerAdvice(context.getBean(ProjectExceptionHandler.class)).build();
    }

    private static final class TestDatabase implements AutoCloseable {

        private final String schema = "project_test_" + UUID.randomUUID().toString().replace("-", "");
        private final String url = Objects.requireNonNull(System.getenv("PROJECT_TEST_DB_URL"));
        private final String username = Objects.requireNonNull(System.getenv("PROJECT_TEST_DB_USERNAME"),
                "Configura PROJECT_TEST_DB_USERNAME para una base PostgreSQL dedicada a pruebas.");
        private final String password = Objects.requireNonNull(System.getenv("PROJECT_TEST_DB_PASSWORD"),
                "Configura PROJECT_TEST_DB_PASSWORD mediante el entorno, sin incluirlo en el repositorio.");
        private final List<ConfigurableApplicationContext> applications = new ArrayList<>();

        ConfigurableApplicationContext open() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("spring.datasource.url", url);
            properties.put("spring.datasource.username", username);
            properties.put("spring.datasource.password", password);
            properties.put("spring.datasource.hikari.schema", schema);
            properties.put("spring.jpa.hibernate.ddl-auto", "validate");
            properties.put("spring.jpa.open-in-view", false);
            properties.put("spring.jpa.properties.hibernate.default_schema", schema);
            properties.put("spring.flyway.enabled", true);
            properties.put("spring.flyway.locations", "classpath:db/migration");
            properties.put("spring.flyway.schemas", schema);
            properties.put("spring.flyway.default-schema", schema);
            properties.put("spring.flyway.create-schemas", true);
            properties.put("spring.sql.init.mode", "never");
            properties.put("spring.main.web-application-type", "none");
            var context = new SpringApplicationBuilder(LlmScannerSeguridadApplication.class)
                    .web(WebApplicationType.NONE).registerShutdownHook(false)
                    .initializers(application -> application.getEnvironment().getPropertySources()
                            .addFirst(new MapPropertySource("project-management-test", properties)))
                    .run();
            applications.add(context);
            return context;
        }

        void closeApplication(ConfigurableApplicationContext context) {
            context.close();
            applications.remove(context);
        }

        @Override
        public void close() throws Exception {
            for (var context : applications) {
                context.close();
            }
            // Solo se elimina el esquema aleatorio creado por esta fixture, nunca tablas compartidas.
            if (!schema.matches("project_test_[0-9a-f]{32}")) {
                throw new IllegalStateException("Esquema de pruebas no válido.");
            }
            try (var connection = DriverManager.getConnection(url, username, password);
                    var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
}
