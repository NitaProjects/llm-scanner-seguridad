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
    public void replace(TargetConfiguration target) {
        try {
            var entity = find(target.projectId()).orElseThrow(() ->
                    new TargetOperationException(TargetOperationException.Reason.TARGET_NOT_CONFIGURED));
            entity.replace(target);
            entityManager.flush();
        } catch (TargetOperationException error) {
            throw error;
        } catch (RuntimeException error) {
            throw TargetOperationException.internal();
        }
    }
}
