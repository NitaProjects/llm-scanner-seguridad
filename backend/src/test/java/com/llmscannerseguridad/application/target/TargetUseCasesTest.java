package com.llmscannerseguridad.application.target;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.llmscannerseguridad.adapters.out.validation.target.JsonTargetConfigurationValidator;
import com.llmscannerseguridad.adapters.out.validation.target.LocalTargetUrlPolicy;
import com.llmscannerseguridad.application.project.ProjectNotFoundException;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.domain.project.Project;
import com.llmscannerseguridad.domain.target.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TargetUseCasesTest {
    private final UUID projectId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-10T10:00:00.123456Z");
    private final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    private final TargetConfigurationRepositoryPort targets = mock(TargetConfigurationRepositoryPort.class);
    private final CredentialEncryptionPort encryption = mock(CredentialEncryptionPort.class);
    private final AtomicReference<TargetConfiguration> saved = new AtomicReference<>();
    private TargetConfigurationService service;
    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project(projectId, "Proyecto", null, now, now);
        when(projects.findById(projectId)).thenReturn(Optional.of(project));
        when(projects.lockForDependentWrite(projectId)).thenReturn(true);
        when(targets.findByProjectId(projectId)).thenAnswer(call -> Optional.ofNullable(saved.get()));
        doAnswer(call -> { saved.set(call.getArgument(0)); return null; }).when(targets).insert(any());
        when(targets.replace(any(), any())).thenAnswer(call -> { saved.set(call.getArgument(0)); return true; });
        when(encryption.encrypt(anyString())).thenAnswer(call -> new EncryptedCredential(new byte[] {1, 2, 3}));
        service = new TargetConfigurationService(projects, targets,
                new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of())),
                encryption, Clock.fixed(now, ZoneOffset.UTC), UUID::randomUUID);
    }

    private TargetCommand command(String url, AuthType type, String header, String secret, boolean present) {
        return new TargetCommand(new TargetSettings(null, new TargetUrl(url), "{\"a\":\"{{PROMPT}}\"}",
                "$.text", Map.of(), type, header, 10000), secret, present);
    }

    @Test
    void creationReadMissingProjectMissingTargetConflictAndNoProjectMutation() {
        var none = command("https://example.test/a", AuthType.NONE, null, null, false);
        var get = new GetTargetConfigurationUseCase(projects, targets);
        assertThatThrownBy(() -> get.execute(projectId)).isInstanceOfSatisfying(TargetOperationException.class,
                error -> assertThat(error.reason()).isEqualTo(TargetOperationException.Reason.TARGET_NOT_CONFIGURED));
        assertThatThrownBy(() -> service.update(projectId, none)).isInstanceOf(TargetOperationException.class);
        var view = service.configure(projectId, none);
        assertThat(view.id()).isNotNull();
        assertThat(view.projectId()).isEqualTo(projectId);
        assertThat(view.createdAt()).isEqualTo(now);
        assertThat(view.updatedAt()).isEqualTo(now);
        assertThat(view.hasCredentials()).isFalse();
        assertThat(get.execute(projectId).id()).isEqualTo(view.id());
        assertThatThrownBy(() -> service.configure(projectId, none)).isInstanceOfSatisfying(TargetOperationException.class,
                error -> assertThat(error.reason()).isEqualTo(TargetOperationException.Reason.TARGET_ALREADY_CONFIGURED));
        verifyNoInteractions(encryption);
        verify(projects, never()).insert(any());
        assertThat(project.updatedAt()).isEqualTo(now);
        UUID missing = UUID.randomUUID();
        when(projects.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.configure(missing, none)).isInstanceOf(ProjectNotFoundException.class);
        assertThatThrownBy(() -> service.update(missing, none)).isInstanceOf(ProjectNotFoundException.class);
        assertThatThrownBy(() -> get.execute(missing)).isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void createAuthenticatedRequiresSecretAndValidationPrecedesEncryptionOrWrite() {
        assertThatThrownBy(() -> service.configure(projectId,
                command("https://example.test", AuthType.BEARER_TOKEN, null, null, false)))
                .isInstanceOf(TargetValidationException.class);
        var invalid = new TargetCommand(new TargetSettings(null, new TargetUrl("https://example.test"),
                "{\"a\":\"no token\"}", "$", Map.of(), AuthType.NONE, null, 10000), null, false);
        assertThatThrownBy(() -> service.configure(projectId, invalid)).isInstanceOf(TargetValidationException.class);
        verifyNoInteractions(encryption);
        verify(targets, never()).insert(any());
        verify(targets, never()).replace(any(), any());
        assertThat(saved.get()).isNull();
        when(encryption.encrypt(anyString())).thenThrow(TargetOperationException.internal());
        assertThatThrownBy(() -> service.configure(projectId,
                command("https://example.test", AuthType.BEARER_TOKEN, null, "sintético", true)))
                .isInstanceOf(TargetOperationException.class);
        verify(targets, never()).insert(any());
        assertThat(saved.get()).isNull();
    }

    @Test
    void preserveWithoutEncryptionRequireNewOnOriginTypeOrExactHeaderChangeAndRemoveAtNone() {
        service.configure(projectId, command("https://EXAMPLE.test/a", AuthType.API_KEY_HEADER, "X-API-Key", " sintético ", true));
        var previous = saved.get();
        clearInvocations(encryption);
        var view = service.update(projectId, command("https://example.test:443/b", AuthType.API_KEY_HEADER,
                "X-API-Key", null, false));
        assertThat(saved.get().credential()).isSameAs(previous.credential());
        assertThat(view.createdAt()).isEqualTo(previous.createdAt());
        assertThat(view.id()).isEqualTo(previous.id());
        new GetTargetConfigurationUseCase(projects, targets).execute(projectId);
        verifyNoInteractions(encryption);
        for (var change : new TargetCommand[] {
                command("http://example.test", AuthType.API_KEY_HEADER, "X-API-Key", null, false),
                command("https://other.test", AuthType.API_KEY_HEADER, "X-API-Key", null, false),
                command("https://example.test:8443", AuthType.API_KEY_HEADER, "X-API-Key", null, false),
                command("https://example.test", AuthType.API_KEY_HEADER, "x-api-key", null, false),
                command("https://example.test", AuthType.BEARER_TOKEN, null, null, false)}) {
            var unchanged = saved.get();
            assertThatThrownBy(() -> service.update(projectId, change)).isInstanceOf(TargetValidationException.class);
            assertThat(saved.get()).isSameAs(unchanged);
        }
        service.update(projectId, command("https://other.test", AuthType.BEARER_TOKEN, null, "nuevo-sintético", true));
        verify(encryption).encrypt("nuevo-sintético");
        assertThat(saved.get().credential()).isNotSameAs(previous.credential());
        assertThat(service.update(projectId, command("https://other.test", AuthType.NONE, null, null, false))
                .hasCredentials()).isFalse();
        assertThat(saved.get().credential()).isNull();
    }

    @Test
    void cipherAndStorageFailuresDoNotBecomeSuccessOrAbsenceAndClockCannotRegress() {
        service.configure(projectId, command("https://example.test", AuthType.BEARER_TOKEN, null, "sintético", true));
        var previous = saved.get();
        saved.set(new TargetConfiguration(previous.id(), projectId, previous.settings(), previous.credential(),
                now, now.plusSeconds(60)));
        service.update(projectId, command("https://example.test/b", AuthType.BEARER_TOKEN, null, null, false));
        assertThat(saved.get().updatedAt()).isEqualTo(now.plusSeconds(60));
        var beforeFailure = saved.get();
        when(encryption.encrypt(anyString())).thenThrow(TargetOperationException.internal());
        assertThatThrownBy(() -> service.update(projectId,
                command("https://example.test", AuthType.BEARER_TOKEN, null, "otro-sintético", true)))
                .isInstanceOf(TargetOperationException.class);
        assertThat(saved.get()).isSameAs(beforeFailure);
        doThrow(TargetOperationException.internal()).when(targets).replace(any(), any());
        assertThatThrownBy(() -> service.update(projectId,
                command("https://example.test", AuthType.NONE, null, null, false)))
                .isInstanceOf(TargetOperationException.class);
        assertThat(saved.get()).isSameAs(beforeFailure);
        when(targets.findByProjectId(projectId)).thenThrow(TargetOperationException.internal());
        assertThatThrownBy(() -> new GetTargetConfigurationUseCase(projects, targets).execute(projectId))
                .isInstanceOfSatisfying(TargetOperationException.class, error ->
                        assertThat(error.reason()).isEqualTo(TargetOperationException.Reason.INTERNAL_ERROR));
    }
}
