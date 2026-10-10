package com.llmscannerseguridad.adapters.out.persistence.scan;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.adapters.out.persistence.target.PostgresTargetConfigurationRepositoryAdapter;
import com.llmscannerseguridad.domain.scan.Scan;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ScanPersistenceTest {
    @Test
    @SuppressWarnings("unchecked")
    void targetExistenceSelectsOnlyIdAndNeverMaterializesCredential() {
        var manager = mock(EntityManager.class);
        var query = (TypedQuery<UUID>) mock(TypedQuery.class);
        var project = UUID.randomUUID();
        when(manager.createQuery(anyString(), eq(UUID.class))).thenAnswer(invocation -> {
            assertThat(invocation.<String>getArgument(0)).isEqualTo(
                    "select t.id from TargetConfigurationJpaEntity t where t.projectId = :projectId");
            return query;
        });
        when(query.setParameter("projectId", project)).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(UUID.randomUUID()), List.of());
        var adapter = new PostgresTargetConfigurationRepositoryAdapter(manager);
        assertThat(adapter.existsByProjectId(project)).isTrue();
        assertThat(adapter.existsByProjectId(project)).isFalse();
        verify(manager, times(2)).createQuery(anyString(), eq(UUID.class));
        verifyNoMoreInteractions(manager);
    }

    @Test
    void entityMapsExactlyTheLifecycleWithoutScoreOrTargetData() {
        var scan = Scan.pending(UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-10-10T08:00:00Z"));
        assertThat(new ScanJpaEntity(scan).toDomain()).isEqualTo(scan);
        assertThat(new ScanJpaEntity(scan.start(scan.createdAt()).complete(scan.createdAt())).toDomain().status())
                .isEqualTo(com.llmscannerseguridad.domain.scan.ScanStatus.COMPLETED);
    }
}
