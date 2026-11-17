package com.llmscannerseguridad.adapters.in.rest.target;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.llmscannerseguridad.adapters.out.crypto.target.AesGcmCredentialEncryptionAdapter;
import com.llmscannerseguridad.adapters.out.validation.target.JsonTargetConfigurationValidator;
import com.llmscannerseguridad.adapters.out.validation.target.LocalTargetUrlPolicy;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.target.*;
import com.llmscannerseguridad.domain.project.Project;
import com.llmscannerseguridad.domain.target.EncryptedCredential;
import com.llmscannerseguridad.domain.target.TargetConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(OutputCaptureExtension.class)
class TargetControllerTest {
    private final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    private final TargetConfigurationRepositoryPort targets = mock(TargetConfigurationRepositoryPort.class);
    private final CredentialEncryptionPort encryption = mock(CredentialEncryptionPort.class);
    private final AtomicReference<TargetConfiguration> saved = new AtomicReference<>();
    private final UUID projectId = UUID.randomUUID();
    private final String route = "/api/projects/" + projectId + "/target";
    private final Instant now = Instant.parse("2026-10-10T10:00:00.123456Z");
    private final JsonMapper json = JsonMapper.builder().build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(projects.findById(projectId)).thenReturn(Optional.of(new Project(projectId, "Proyecto", null, now, now)));
        when(projects.lockForDependentWrite(projectId)).thenReturn(true);
        when(targets.findByProjectId(projectId)).thenAnswer(call -> Optional.ofNullable(saved.get()));
        doAnswer(call -> { saved.set(call.getArgument(0)); return null; }).when(targets).insert(any());
        when(targets.replace(any(), any())).thenAnswer(call -> { saved.set(call.getArgument(0)); return true; });
        when(encryption.encrypt(anyString())).thenReturn(new EncryptedCredential(new byte[] {1, 2, 3}));
        var service = new TargetConfigurationService(projects, targets,
                new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of())),
                encryption, Clock.fixed(now, ZoneOffset.UTC), UUID::randomUUID);
        mvc = MockMvcBuilders.standaloneSetup(new TargetController(service::configure,
                new GetTargetConfigurationUseCase(projects, targets), service::update))
                .setControllerAdvice(new TargetExceptionHandler()).build();
    }

    private Map<String, Object> input() {
        return new LinkedHashMap<>(Map.of("url", "https://example.test/chat",
                "requestTemplate", "{\"message\":\"{{PROMPT}}\"}", "responsePath", "$.text", "authType", "NONE"));
    }

    private Map<String, Object> chatInput() {
        return new LinkedHashMap<>(Map.of("adapterType", "OPENAI_COMPATIBLE", "endpoint", "https://chat.test/completions",
                "model", " fixture-model ", "systemPrompt", "synthetic system", "authType", "BEARER_TOKEN",
                "authConfig", Map.of("secretValue", "synthetic-chat")));
    }

    @Test
    void chatWithNullSystemPromptRequiresLocalEncryptionKeyBeforeWriting(CapturedOutput output) throws Exception {
        var key = new AtomicReference<String>();
        var service = new TargetConfigurationService(projects, targets,
                new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of())),
                new AesGcmCredentialEncryptionAdapter(key::get), Clock.fixed(now, ZoneOffset.UTC), UUID::randomUUID);
        var controller = MockMvcBuilders.standaloneSetup(new TargetController(service::configure,
                new GetTargetConfigurationUseCase(projects, targets), service::update))
                .setControllerAdvice(new TargetExceptionHandler()).build();
        var fields = chatInput();
        fields.put("endpoint", "https://api.groq.com/openai/v1/chat/completions");
        fields.put("model", "qwen/qwen3.8-27b");
        fields.put("systemPrompt", null);
        fields.put("timeoutMs", 20000);

        controller.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        verify(targets, never()).insert(any());

        key.set(Base64.getEncoder().encodeToString(new byte[32]));
        controller.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.hasCredentials").value(true));
        assertThat(saved.get().settings().systemPrompt()).isNull();
        assertThat(saved.get().settings().timeoutMs()).isEqualTo(20000);
        assertThat(output.getAll()).doesNotContain("synthetic-chat", key.get());
    }

    @Test
    void chatContractReadsExplicitColumnsAndPreservesCredentialWithoutLegacyFields() throws Exception {
        var fields = chatInput();
        mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.adapterType").value("OPENAI_COMPATIBLE"))
                .andExpect(jsonPath("$.endpoint").value("https://chat.test/completions"))
                .andExpect(jsonPath("$.model").value("fixture-model"))
                .andExpect(jsonPath("$.requestTemplate").doesNotExist()).andExpect(jsonPath("$.responsePath").doesNotExist())
                .andExpect(jsonPath("$.headers").doesNotExist()).andExpect(jsonPath("$.url").doesNotExist())
                .andExpect(jsonPath("$.authConfig").value(nullValue()));
        var credential = saved.get().credential();
        assertThat(saved.get().settings().requestTemplate()).isNull();assertThat(saved.get().settings().responsePath()).isNull();
        fields.put("authConfig", Map.of());fields.put("model", "new-model");
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(fields))).andExpect(status().isOk());
        assertThat(saved.get().credential()).isSameAs(credential);
        mvc.perform(get(route)).andExpect(status().isOk()).andExpect(jsonPath("$.model").value("new-model"));
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(input())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TARGET_TYPE_REQUIRED"));
    }

    @Test
    void chatRejectsExtrasMissingModelWrongAuthAndConditionalWriteConflict() throws Exception {
        for (String field : List.of("requestTemplate", "responsePath", "headers", "temperature")) {
            var fields=chatInput();fields.put(field,"unaccepted");
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        for(String model:List.of("", " ", "{{PROMPT}}", "m".repeat(201))) {
            var fields=chatInput();fields.put("model",model);
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields))).andExpect(status().isBadRequest());
        }
        var fields=chatInput();fields.put("authType","NONE");fields.remove("authConfig");
        mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields))).andExpect(status().isBadRequest());
        mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(input()))).andExpect(status().isCreated());
        var before=saved.get();doReturn(false).when(targets).replace(any(),any());
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(chatInput())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TARGET_TYPE_CHANGED"));
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(input())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TARGET_TYPE_REQUIRED"));
        assertThat(saved.get()).isSameAs(before);
    }

    private String body(Map<String, Object> fields) {
        return json.writeValueAsString(fields);
    }

    @Test
    void createsReadsUpdatesWithExactSafeFieldsDefaultsDatesLocationAndConflict() throws Exception {
        mvc.perform(get(route)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TARGET_NOT_CONFIGURED"));
        var fields = input();
        fields.put("timeoutMs", 60000);
        mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                .andExpect(status().isCreated()).andExpect(header().string("Location", route))
                .andExpect(jsonPath("$", aMapWithSize(16)))
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.contentType").value("application/json"))
                .andExpect(jsonPath("$.promptPlaceholder").value("{{PROMPT}}"))
                .andExpect(jsonPath("$.name").value(nullValue()))
                .andExpect(jsonPath("$.headers", aMapWithSize(0)))
                .andExpect(jsonPath("$.authConfig").value(nullValue()))
                .andExpect(jsonPath("$.hasCredentials").value(false))
                .andExpect(jsonPath("$.createdAt").value(now.toString()))
                .andExpect(jsonPath("$.updatedAt").value(now.toString()));
        var original = saved.get();
        mvc.perform(get(route)).andExpect(status().isOk()).andExpect(jsonPath("$.timeoutMs").value(60000));
        mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(input())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TARGET_ALREADY_CONFIGURED"));
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(input())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeoutMs").value(10000))
                .andExpect(jsonPath("$.id").value(original.id().toString()))
                .andExpect(jsonPath("$.createdAt").value(original.createdAt().toString()));
        verifyNoInteractions(encryption);
    }

    @Test
    void credentialsAreWriteOnlyPreservedReplacedRemovedAndAbsentFromLogs(CapturedOutput output) throws Exception {
        String marker = "sintético-" + UUID.randomUUID();
        var fields = input();
        fields.put("authType", "API_KEY_HEADER");
        fields.put("authConfig", Map.of("headerName", "X-API-Key", "secretValue", marker));
        var response = mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.hasCredentials").value(true))
                .andExpect(jsonPath("$.timeoutMs").value(10000))
                .andExpect(jsonPath("$.authConfig", aMapWithSize(1)))
                .andExpect(jsonPath("$.authConfig.headerName").value("X-API-Key")).andReturn();
        assertThat(response.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain(marker, "secretValue", "ciphertext", "nonce", "envelope");
        var original = saved.get().credential();
        clearInvocations(encryption);
        fields.put("authConfig", Map.of("headerName", "X-API-Key"));
        fields.put("url", "https://EXAMPLE.test:443/other");
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(fields))).andExpect(status().isOk());
        mvc.perform(get(route)).andExpect(status().isOk()).andExpect(jsonPath("$.hasCredentials").value(true))
                .andExpect(jsonPath("$.authConfig.secretValue").doesNotExist());
        assertThat(saved.get().credential()).isSameAs(original);
        verifyNoInteractions(encryption);
        fields.put("url", "https://another.test");
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("authConfig.secretValue"));
        fields.put("authConfig", Map.of("headerName", "X-API-Key", "secretValue", marker));
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(fields))).andExpect(status().isOk());
        verify(encryption).encrypt(marker);
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(input())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hasCredentials").value(false));
        assertThat(saved.get().credential()).isNull();
        assertThat(output.getAll()).doesNotContain(marker);
    }

    @Test
    void strictStructureUnknownFieldsDuplicateKeysAndGeneratedFieldsAreInvalidRequests() throws Exception {
        for (String value : List.of("", "{", "[]", "null", "{} {}", "{\"url\":\"a\",\"url\":\"b\"}")) {
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(value))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
        }
        for (String field : List.of("id", "projectId", "createdAt", "updatedAt", "hasCredentials", "allowPrivateTargets")) {
            var fields = input();
            fields.put(field, true);
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        var fields = input();
        fields.put("authType", "BEARER_TOKEN");
        fields.put("authConfig", Map.of("unknown", "sintético"));
        mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        fields.put("authType", "NONE");
        mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(projects, targets, encryption);
    }

    @Test
    void validatesTypesConstantsTimeoutAuthNullHeadersAndEscapedNullWithoutPersistence(CapturedOutput output) throws Exception {
        for (String field : List.of("url", "requestTemplate", "responsePath", "authType")) {
            var fields = input();
            fields.remove(field);
            invalid(fields, field);
            fields = input();
            fields.put(field, 42);
            invalid(fields, field);
        }
        for (Object timeout : new Object[] {null, 999, 60001, 1000.0, "1000", true}) {
            var fields = input();
            fields.put("timeoutMs", timeout);
            invalid(fields, "timeoutMs");
        }
        for (String field : List.of("method", "contentType", "promptPlaceholder")) {
            var fields = input();
            fields.put(field, "otro");
            invalid(fields, field);
        }
        for (String field : List.of("name", "url", "requestTemplate", "responsePath", "authType")) {
            var fields = input();
            fields.put(field, "marcador-privado\0fin");
            var result = mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value(field))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_CHARACTER")).andReturn();
            assertThat(result.getResponse().getContentAsString()).doesNotContain("marcador-privado");
        }
        for (Object headerValue : new Object[] {null, "bad", List.of("a"), Map.of("Authorization", "marcador"),
                Map.of("Accept", "a", "accept", "b"), Map.of("X-Test", "bad\r\nvalue")}) {
            var fields = input();
            fields.put("headers", headerValue);
            invalid(fields, "headers");
        }
        for (String authType : List.of("NONE", "BEARER_TOKEN", "API_KEY_HEADER", "OAUTH2")) {
            var fields = input();
            fields.put("authType", authType);
            fields.put("authConfig", Map.of());
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        for (Object secret : new Object[] {null, "", " ", "with space", "bad\0value", 123}) {
            var fields = input();
            fields.put("authType", "BEARER_TOKEN");
            var auth = new LinkedHashMap<String, Object>();
            auth.put("secretValue", secret);
            fields.put("authConfig", auth);
            invalid(fields, "authConfig.secretValue");
        }
        verify(targets, never()).insert(any());
        verify(targets, never()).replace(any(), any());
        verifyNoInteractions(encryption);
        assertThat(output.getAll()).doesNotContain("marcador-privado", "bad\r\nvalue");
    }

    @Test
    void identifiersMissingProjectMissingTargetAndTechnicalFailuresAreSanitized() throws Exception {
        mvc.perform(get("/api/projects/no-uuid/target")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PROJECT_ID"));
        UUID missing = UUID.randomUUID();
        when(projects.findById(missing)).thenReturn(Optional.empty());
        for (var request : List.of(get("/api/projects/" + missing + "/target"),
                post("/api/projects/" + missing + "/target").contentType(MediaType.APPLICATION_JSON).content(body(input())),
                put("/api/projects/" + missing + "/target").contentType(MediaType.APPLICATION_JSON).content(body(input())))) {
            mvc.perform(request).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
        }
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content(body(input())))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TARGET_NOT_CONFIGURED"));
        when(targets.findByProjectId(projectId)).thenThrow(TargetOperationException.internal());
        mvc.perform(get(route)).andExpect(status().isInternalServerError())
                .andExpect(content().json("""
                    {"code":"INTERNAL_ERROR","message":"No se ha podido completar la operación.","fieldErrors":[]}
                    """));
    }

    @Test
    void isolatedSurrogatesAreRejectedBeforeEncryptionAndPersistence(CapturedOutput output) throws Exception {
        var fields = input();
        fields.put("authType", "BEARER_TOKEN");
        fields.put("authConfig", Map.of("secretValue", "private-marker-SURROGATE-end"));
        for (String escape : List.of("\\uD800", "\\uDC00")) {
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON)
                            .content(body(fields).replace("SURROGATE", escape)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("""
                        {"code":"VALIDATION_ERROR","message":"Los datos del target no son válidos.",
                         "fieldErrors":[{"field":"authConfig.secretValue","code":"INVALID_CHARACTER",
                         "message":"El campo no cumple las reglas de configuración."}]}
                        """));
        }
        verifyNoInteractions(projects, targets, encryption);
        assertThat(saved.get()).isNull();
        assertThat(output.getAll()).doesNotContain("private-marker");
    }

    @Test
    void invalidSurrogatePutPreservesPreviousConfigurationAndCredential(CapturedOutput output) throws Exception {
        var fields = input();
        fields.put("authType", "API_KEY_HEADER");
        fields.put("authConfig", Map.of("headerName", "X-API-Key", "secretValue", "synthetic-original"));
        mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                .andExpect(status().isCreated());
        var previous = saved.get();
        clearInvocations(projects, targets, encryption);
        fields.put("name", "Cambio rechazado");
        fields.put("url", "https://another.test");
        fields.put("authConfig", Map.of("headerName", "X-API-Key", "secretValue", "private-marker-SURROGATE-end"));
        for (String escape : List.of("\\uD800", "\\uDC00")) {
            mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON)
                            .content(body(fields).replace("SURROGATE", escape)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("""
                        {"code":"VALIDATION_ERROR","message":"Los datos del target no son válidos.",
                         "fieldErrors":[{"field":"authConfig.secretValue","code":"INVALID_CHARACTER",
                         "message":"El campo no cumple las reglas de configuración."}]}
                        """));
            assertThat(saved.get()).isSameAs(previous);
            assertThat(saved.get().credential()).isSameAs(previous.credential());
        }
        verifyNoInteractions(projects, targets, encryption);
        assertThat(output.getAll()).doesNotContain("private-marker");
    }

    @Test
    void validSupplementaryPairReachesEncryptionUnchanged() throws Exception {
        var fields = input();
        fields.put("authType", "BEARER_TOKEN");
        fields.put("authConfig", Map.of("secretValue", "A-SURROGATE-B"));
        mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON)
                        .content(body(fields).replace("SURROGATE", "\\uD83D\\uDD12")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authConfig").value(nullValue()))
                .andExpect(jsonPath("$.hasCredentials").value(true));
        verify(encryption).encrypt("A-\uD83D\uDD12-B");
        verify(targets).insert(any());
    }

    private void invalid(Map<String, Object> fields, String field) throws Exception {
        mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(fields)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
    }
}
