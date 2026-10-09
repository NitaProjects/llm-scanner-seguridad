package com.llmscannerseguridad.application.project;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.llmscannerseguridad.domain.project.Project;
import com.llmscannerseguridad.domain.project.ProjectValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectUseCasesTest {

    private final ProjectRepositoryPort repository = mock(ProjectRepositoryPort.class);
    private final Instant now = Instant.parse("2026-10-09T10:00:00.123456789Z");
    private final CreateProjectService create = new CreateProjectService(
            repository, Clock.fixed(now, ZoneOffset.UTC), UUID::randomUUID);

    @Test
    void createsNormalizedProjectsWithOneMicrosecondInstantAndDistinctIdentifiers() {
        var first = create.execute(new CreateProjectCommand(" Proyecto ", " Descripción "));
        var second = create.execute(new CreateProjectCommand("Proyecto", null));
        assertThat(first.name()).isEqualTo("Proyecto");
        assertThat(first.description()).isEqualTo("Descripción");
        assertThat(second.description()).isNull();
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.createdAt()).isEqualTo(Instant.parse("2026-10-09T10:00:00.123456Z"));
        assertThat(first.updatedAt()).isEqualTo(first.createdAt());
        verify(repository).insert(first);
        verify(repository).insert(second);
    }

    @Test
    void invalidDataNeverReachesPersistence() {
        assertThatThrownBy(() -> create.execute(new CreateProjectCommand(" ", null)))
                .isInstanceOf(ProjectValidationException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void getsAnExistingProjectWithoutChangingDatesAndDistinguishesAbsence() {
        var project = project(UUID.randomUUID(), now);
        when(repository.findById(project.id())).thenReturn(Optional.of(project));
        assertThat(new GetProjectUseCase(repository).execute(project.id())).isEqualTo(project);
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new GetProjectUseCase(repository).execute(missing))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void listsAllProjectsInThePortContractOrderIncludingTemporalTies() {
        var older = project(UUID.randomUUID(), now.minusSeconds(1));
        var low = project(UUID.fromString("00000000-0000-0000-0000-000000000001"), now);
        var high = project(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), now);
        when(repository.findAllOrdered()).thenReturn(List.of(low, high, older));
        assertThat(new ListProjectsUseCase(repository).execute()).containsExactly(low, high, older);
        when(repository.findAllOrdered()).thenReturn(List.of());
        assertThat(new ListProjectsUseCase(repository).execute()).isEmpty();
    }

    @Test
    void storageFailuresAreNeverSuccessAbsenceOrEmptyList() {
        var failure = new ProjectPersistenceException(new IllegalStateException("Fallo simulado"));
        doThrow(failure).when(repository).insert(any());
        when(repository.findById(any())).thenThrow(failure);
        when(repository.findAllOrdered()).thenThrow(failure);
        assertThatThrownBy(() -> create.execute(new CreateProjectCommand("Proyecto", null))).isSameAs(failure);
        assertThatThrownBy(() -> new GetProjectUseCase(repository).execute(UUID.randomUUID())).isSameAs(failure);
        assertThatThrownBy(() -> new ListProjectsUseCase(repository).execute()).isSameAs(failure);
    }

    private Project project(UUID id, Instant instant) {
        return new Project(id, "Proyecto", null, instant, instant);
    }
}
