package com.llmscannerseguridad.integration;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.llmscannerseguridad.LlmScannerSeguridadApplication;
import com.llmscannerseguridad.adapters.in.rest.target.TargetController;
import com.llmscannerseguridad.adapters.in.rest.target.TargetExceptionHandler;
import com.llmscannerseguridad.application.project.CreateProjectCommand;
import com.llmscannerseguridad.application.project.CreateProjectUseCase;
import com.llmscannerseguridad.application.project.GetProjectUseCase;
import com.llmscannerseguridad.application.target.*;
import com.llmscannerseguridad.config.TargetTransactions;
import com.llmscannerseguridad.domain.project.Project;
import com.llmscannerseguridad.domain.target.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Solo con autorización explícita de PostgreSQL y migraciones en una base dedicada.
// No contactar targets. La fixture crea y elimina únicamente su esquema aleatorio.
@EnabledIfEnvironmentVariable(named = "TARGET_POSTGRES_TESTS_ENABLED", matches = "true")
@EnabledIfEnvironmentVariable(named = "TARGET_TEST_DB_URL", matches = "jdbc:postgresql:.+")
@ExtendWith(OutputCaptureExtension.class)
class TargetPostgresIntegrationTest {
    @Test
    void authenticatedConfigurationPersistsRecoversAndPreservesWithoutKeyAfterRestart(CapturedOutput output) throws Exception {
        try (var db = new TestDatabase()) {
            var first = db.open(db.encodedKey);
            var project = project(first);
            String route = route(project);
            String marker = "sintético-" + UUID.randomUUID();
            var fields = input();
            fields.put("authType", "API_KEY_HEADER");
            fields.put("authConfig", Map.of("headerName", "X-API-Key", "secretValue", marker));
            mvc(first).perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                    .andExpect(status().isCreated()).andExpect(header().string("Location", route))
                    .andExpect(jsonPath("$.hasCredentials").value(true))
                    .andExpect(jsonPath("$.authConfig.secretValue").doesNotExist());
            var jdbc = first.getBean(JdbcTemplate.class);
            byte[] ciphertext = jdbc.queryForObject("SELECT credential_ciphertext FROM target_configurations", byte[].class);
            assertThat(ciphertext).isNotNull();
            assertThat(decrypt(ciphertext, db.key)).isEqualTo(marker);
            assertThat(jdbc.queryForObject("SELECT headers::text FROM target_configurations", String.class))
                    .doesNotContain(marker, "secretValue");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema = ? "
                    + "AND table_name = 'target_configurations' AND column_name IN ('method','content_type','prompt_placeholder')",
                    Long.class, db.schema)).isZero();
            var before = first.getBean(GetTargetConfigurationUseCase.class).execute(project.id());
            db.closeApplication(first);

            // Una nueva instancia conserva la base y no necesita clave para GET/PUT sin secreto nuevo.
            var second = db.open("");
            var response = mvc(second).perform(get(route)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasCredentials").value(true)).andReturn();
            assertThat(response.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .doesNotContain(marker, "secretValue", "ciphertext", "nonce");
            fields.put("authConfig", Map.of("headerName", "X-API-Key"));
            fields.put("url", "https://EXAMPLE.test:443/another");
            mvc(second).perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.timeoutMs").value(10000));
            var after = second.getBean(GetTargetConfigurationUseCase.class).execute(project.id());
            assertThat(after.id()).isEqualTo(before.id());
            assertThat(after.createdAt()).isEqualTo(before.createdAt());
            assertThat(after.updatedAt()).isAfterOrEqualTo(before.updatedAt());
            byte[] preserved = second.getBean(JdbcTemplate.class)
                    .queryForObject("SELECT credential_ciphertext FROM target_configurations", byte[].class);
            assertThat(preserved).containsExactly(ciphertext);
            assertThat(decrypt(preserved, db.key)).isEqualTo(marker);
            assertThat(second.getBean(GetProjectUseCase.class).execute(project.id())).isEqualTo(project);
            assertThat(output.getAll()).doesNotContain(marker, Base64.getEncoder().encodeToString(ciphertext));
        }
    }

    @Test
    void validationAbsenceConflictAndRemovalPreserveProjectsWithoutPartialRows() throws Exception {
        try (var db = new TestDatabase()) {
            var context = db.open(db.encodedKey);
            var project = project(context);
            var mvc = mvc(context);
            String route = route(project);
            mvc.perform(get(route)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TARGET_NOT_CONFIGURED"));
            mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(input())))
                    .andExpect(status().isNotFound());
            mvc.perform(post("/api/projects/" + UUID.randomUUID() + "/target")
                    .contentType(MediaType.APPLICATION_JSON).content(body(input())))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
            mvc.perform(get("/api/projects/invalid/target")).andExpect(status().isBadRequest());
            for (String field : List.of("name", "url", "requestTemplate", "responsePath")) {
                var invalid = input();
                invalid.put(field, "marcador\0fin");
                mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(invalid)))
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_CHARACTER"));
            }
            var jdbc = context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM target_configurations", Long.class)).isZero();
            var fields = input();
            fields.put("authType", "BEARER_TOKEN");
            fields.put("authConfig", Map.of("secretValue", "sintético-" + UUID.randomUUID()));
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields))).andExpect(status().isCreated());
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(input())))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TARGET_ALREADY_CONFIGURED"));
            var before = context.getBean(TargetConfigurationRepositoryPort.class).findByProjectId(project.id()).orElseThrow();
            fields.put("authConfig", Map.of());
            mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.hasCredentials").value(true))
                    .andExpect(jsonPath("$.authConfig").value(org.hamcrest.Matchers.nullValue()));
            fields.put("url", "https://another.test");
            mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(fields))).andExpect(status().isBadRequest());
            assertThat(jdbc.queryForObject("SELECT credential_ciphertext FROM target_configurations", byte[].class))
                    .containsExactly(before.credential().bytes());
            fields.put("authConfig", Map.of("secretValue", "nuevo-sintético-" + UUID.randomUUID()));
            mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(fields))).andExpect(status().isOk());
            assertThat(jdbc.queryForObject("SELECT credential_ciphertext FROM target_configurations", byte[].class))
                    .isNotEqualTo(before.credential().bytes());
            mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(input())))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.hasCredentials").value(false));
            assertThat(jdbc.queryForObject("SELECT credential_ciphertext FROM target_configurations", byte[].class)).isNull();
            assertThat(context.getBean(GetProjectUseCase.class).execute(project.id())).isEqualTo(project);
        }
    }

    @Test
    void concurrentInsertsAreEnforcedByDatabaseUniqueAndTranslatedToFunctionalConflict() throws Exception {
        try (var db = new TestDatabase()) {
            var context = db.open(db.encodedKey);
            var project = project(context);
            var targets = context.getBean(TargetConfigurationRepositoryPort.class);
            var transactions = new TargetTransactions(context.getBean(PlatformTransactionManager.class));
            var bothAbsent = new CountDownLatch(2);
            try (var pool = Executors.newFixedThreadPool(2)) {
                var jobs = new ArrayList<java.util.concurrent.Future<String>>();
                for (int i = 0; i < 2; i++) {
                    jobs.add(pool.submit(() -> {
                        try {
                            return transactions.execute(() -> {
                                assertThat(targets.findByProjectId(project.id())).isEmpty();
                                bothAbsent.countDown();
                                try {
                                    if (!bothAbsent.await(15, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("Barrera de pruebas no alcanzada.");
                                    }
                                } catch (InterruptedException error) {
                                    Thread.currentThread().interrupt();
                                    throw new IllegalStateException("Prueba interrumpida.");
                                }
                                Instant now = Instant.parse("2026-10-10T10:00:00Z");
                                var settings = new TargetSettings(null, new TargetUrl("https://example.test"),
                                        "{\"a\":\"{{PROMPT}}\"}", "$", Map.of(), AuthType.NONE, null, 10000);
                                targets.insert(new TargetConfiguration(UUID.randomUUID(), project.id(), settings, null, now, now));
                                return "CREATED";
                            });
                        } catch (TargetOperationException error) {
                            return error.reason().name();
                        }
                    }));
                }
                assertThat(List.of(jobs.get(0).get(30, TimeUnit.SECONDS), jobs.get(1).get(30, TimeUnit.SECONDS)))
                        .containsExactlyInAnyOrder("CREATED", "TARGET_ALREADY_CONFIGURED");
            }
            assertThat(context.getBean(JdbcTemplate.class).queryForObject("SELECT count(*) FROM target_configurations", Long.class))
                    .isEqualTo(1);
        }
    }

    @Test
    void missingKeyAndDeferredCommitFailureLeaveConfigurationAndCredentialUnchanged() throws Exception {
        try (var db = new TestDatabase()) {
            var first = db.open(db.encodedKey);
            var project = project(first);
            var fields = input();
            fields.put("authType", "BEARER_TOKEN");
            fields.put("authConfig", Map.of("secretValue", "sintético-" + UUID.randomUUID()));
            mvc(first).perform(post(route(project)).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                    .andExpect(status().isCreated());
            byte[] before = first.getBean(JdbcTemplate.class)
                    .queryForObject("SELECT credential_ciphertext FROM target_configurations", byte[].class);
            db.closeApplication(first);
            var context = db.open("");
            var withoutTarget = project(context);
            mvc(context).perform(post(route(withoutTarget)).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                    .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
            assertThat(context.getBean(JdbcTemplate.class)
                    .queryForObject("SELECT count(*) FROM target_configurations", Long.class)).isEqualTo(1);
            fields.put("name", "Cambio que no debe persistirse");
            fields.put("authConfig", Map.of("secretValue", "reemplazo-sintético-" + UUID.randomUUID()));
            mvc(context).perform(put(route(project)).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                    .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
            var jdbc = context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT name FROM target_configurations", String.class)).isNull();
            assertThat(jdbc.queryForObject("SELECT credential_ciphertext FROM target_configurations", byte[].class))
                    .containsExactly(before);
            jdbc.execute("""
                    CREATE FUNCTION reject_target_commit() RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN RAISE EXCEPTION 'Fallo de commit sintetico'; RETURN NEW; END; $$
                    """);
            jdbc.execute("""
                    CREATE CONSTRAINT TRIGGER reject_target_commit AFTER UPDATE ON target_configurations
                    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION reject_target_commit()
                    """);
            // La retirada de credencial también debe revertirse si falla el commit.
            mvc(context).perform(put(route(project)).contentType(MediaType.APPLICATION_JSON).content(body(input())))
                    .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.fieldErrors").isEmpty());
            assertThat(jdbc.queryForObject("SELECT auth_type FROM target_configurations", String.class)).isEqualTo("BEARER_TOKEN");
            assertThat(jdbc.queryForObject("SELECT credential_ciphertext FROM target_configurations", byte[].class))
                    .containsExactly(before);
        }
    }

    @Test
    void readFailureIsInternalErrorNeverAbsence() throws Exception {
        try (var db = new TestDatabase()) {
            var context = db.open(db.encodedKey);
            var project = project(context);
            context.getBean(JdbcTemplate.class).execute("ALTER TABLE target_configurations RENAME TO target_unavailable");
            mvc(context).perform(get(route(project))).andExpect(status().isInternalServerError())
                    .andExpect(content().json("""
                            {"code":"INTERNAL_ERROR","message":"No se ha podido completar la operación.","fieldErrors":[]}
                            """));
        }
    }

    private Project project(ConfigurableApplicationContext context) {
        return context.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("Proyecto de integración", null));
    }

    private String route(Project project) {
        return "/api/projects/" + project.id() + "/target";
    }

    private Map<String, Object> input() {
        return new HashMap<>(Map.of("url", "https://example.test/chat",
                "requestTemplate", "{\"message\":\"{{PROMPT}}\"}", "responsePath", "$.text", "authType", "NONE"));
    }

    private String body(Map<String, Object> value) {
        return JsonMapper.builder().build().writeValueAsString(value);
    }

    private MockMvc mvc(ConfigurableApplicationContext context) {
        return MockMvcBuilders.standaloneSetup(context.getBean(TargetController.class))
                .setControllerAdvice(context.getBean(TargetExceptionHandler.class)).build();
    }

    private String decrypt(byte[] envelope, byte[] key) throws Exception {
        assertThat(envelope[0]).isEqualTo((byte) 1);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, Arrays.copyOfRange(envelope, 1, 13)));
        cipher.updateAAD(new byte[] {1});
        return new String(cipher.doFinal(Arrays.copyOfRange(envelope, 13, envelope.length)), StandardCharsets.UTF_8);
    }

    private static final class TestDatabase implements AutoCloseable {
        private final String schema = "target_test_" + UUID.randomUUID().toString().replace("-", "");
        private final String url = Objects.requireNonNull(System.getenv("TARGET_TEST_DB_URL"));
        private final String username = Objects.requireNonNull(System.getenv("TARGET_TEST_DB_USERNAME"),
                "Configura externamente TARGET_TEST_DB_USERNAME para una base dedicada.");
        private final String password = Objects.requireNonNull(System.getenv("TARGET_TEST_DB_PASSWORD"),
                "Configura externamente TARGET_TEST_DB_PASSWORD, sin incluirlo en el repositorio.");
        private final byte[] key = new byte[32];
        private final String encodedKey;
        private final List<ConfigurableApplicationContext> applications = new ArrayList<>();

        TestDatabase() {
            new SecureRandom().nextBytes(key);
            encodedKey = Base64.getEncoder().encodeToString(key);
        }

        ConfigurableApplicationContext open(String syntheticKey) {
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
            properties.put("TARGET_CREDENTIAL_ENCRYPTION_KEY", syntheticKey);
            properties.put("TARGET_LOCAL_DEMO_ENABLED", "false");
            var context = new SpringApplicationBuilder(LlmScannerSeguridadApplication.class)
                    .web(WebApplicationType.NONE).registerShutdownHook(false)
                    .initializers(application -> application.getEnvironment().getPropertySources()
                            .addFirst(new MapPropertySource("target-configuration-test", properties))).run();
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
            Arrays.fill(key, (byte) 0);
            if (!schema.matches("target_test_[0-9a-f]{32}")) {
                throw new IllegalStateException("Esquema de pruebas no válido.");
            }
            try (var connection = DriverManager.getConnection(url, username, password);
                    var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
}
