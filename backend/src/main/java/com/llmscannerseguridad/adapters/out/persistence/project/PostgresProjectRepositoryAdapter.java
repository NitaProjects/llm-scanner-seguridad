package com.llmscannerseguridad.adapters.out.persistence.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.llmscannerseguridad.application.project.ProjectPersistenceException;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.domain.project.Project;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

public class PostgresProjectRepositoryAdapter implements ProjectRepositoryPort {

    private final EntityManager entityManager;

    public PostgresProjectRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void insert(Project project) {
        try {
            entityManager.persist(new ProjectJpaEntity(project));
            entityManager.flush();
        } catch (RuntimeException error) {
            throw new ProjectPersistenceException(error);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findById(UUID id) {
        try {
            return Optional.ofNullable(entityManager.find(ProjectJpaEntity.class, id))
                    .map(ProjectJpaEntity::toDomain);
        } catch (RuntimeException error) {
            throw new ProjectPersistenceException(error);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findAllOrdered() {
        try {
            // PostgreSQL ordena UUID por sus bytes sin signo, igual que su texto canónico.
            return entityManager.createQuery(
                    "select p from ProjectJpaEntity p order by p.createdAt desc, p.id asc",
                    ProjectJpaEntity.class).getResultList().stream().map(ProjectJpaEntity::toDomain).toList();
        } catch (RuntimeException error) {
            throw new ProjectPersistenceException(error);
        }
    }
}
