package com.llmscannerseguridad.adapters.out.persistence.target;

import java.util.Optional;
import java.util.UUID;
import com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort;
import com.llmscannerseguridad.application.target.TargetOperationException;
import com.llmscannerseguridad.domain.target.TargetConfiguration;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.transaction.annotation.Transactional;

public class PostgresTargetConfigurationRepositoryAdapter implements TargetConfigurationRepositoryPort {
    private final EntityManager entityManager;

    public PostgresTargetConfigurationRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByProjectId(UUID projectId) {
        try {
            // Proyección escalar: no materializa configuración ni credential_ciphertext.
            return !entityManager.createQuery(
                            "select t.id from TargetConfigurationJpaEntity t where t.projectId = :projectId", UUID.class)
                    .setParameter("projectId", projectId).setMaxResults(1).getResultList().isEmpty();
        } catch (RuntimeException error) {
            throw TargetOperationException.internal();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TargetConfiguration> findByProjectId(UUID projectId) {
        try {
            return find(projectId).map(TargetConfigurationJpaEntity::toDomain);
        } catch (RuntimeException error) {
            throw TargetOperationException.internal();
        }
    }

    private Optional<TargetConfigurationJpaEntity> find(UUID projectId) {
        return entityManager.createQuery("select t from TargetConfigurationJpaEntity t where t.projectId = :projectId",
                        TargetConfigurationJpaEntity.class)
                .setParameter("projectId", projectId).getResultList().stream().findFirst();
    }

    @Override
    public void insert(TargetConfiguration target) {
        try {
            entityManager.persist(new TargetConfigurationJpaEntity(target));
            entityManager.flush();
        } catch (RuntimeException error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "23505".equals(violation.getSQLState())
                        && "uq_target_configurations_project_id".equals(violation.getConstraintName())) {
                    throw new TargetOperationException(TargetOperationException.Reason.TARGET_ALREADY_CONFIGURED);
                }
            }
            throw TargetOperationException.internal();
        }
    }

    @Override
    public boolean replace(TargetConfiguration target, com.llmscannerseguridad.domain.target.TargetAdapterType expectedAdapterType) {
        try {
            var s = target.settings();
            int changed = entityManager.createQuery("""
                    update TargetConfigurationJpaEntity t set t.name=:name, t.url=:url,
                        t.requestTemplate=:template, t.responsePath=:path, t.headers=:headers,
                        t.authType=:auth, t.authHeaderName=:header, t.credentialCiphertext=:credential,
                        t.timeoutMs=:timeout, t.updatedAt=:updated, t.adapterType=:type,
                        t.model=:model, t.systemPrompt=:system
                    where t.id=:id and t.projectId=:project and t.adapterType=:expected
                    """).setParameter("name", s.name()).setParameter("url", s.url().value())
                    .setParameter("template", s.requestTemplate()).setParameter("path", s.responsePath())
                    .setParameter("headers", s.headers()).setParameter("auth", s.authType().name())
                    .setParameter("header", s.authHeaderName())
                    .setParameter("credential", target.credential() == null ? null : target.credential().bytes())
                    .setParameter("timeout", s.timeoutMs()).setParameter("updated", target.updatedAt())
                    .setParameter("type", s.adapterType().name()).setParameter("model", s.model())
                    .setParameter("system", s.systemPrompt()).setParameter("id", target.id())
                    .setParameter("project", target.projectId()).setParameter("expected", expectedAdapterType.name())
                    .executeUpdate();
            entityManager.clear();
            return changed == 1;
        } catch (TargetOperationException error) {
            throw error;
        } catch (RuntimeException error) {
            throw TargetOperationException.internal();
        }
    }
}
