package com.llmscannerseguridad.adapters.out.persistence.scan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.llmscannerseguridad.application.scan.ScanRepositoryPort;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

public class PostgresScanRepositoryAdapter implements ScanRepositoryPort {
    private final EntityManager entityManager;

    public PostgresScanRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void insert(Scan scan) {
        try {
            entityManager.persist(new ScanJpaEntity(scan));
            entityManager.flush();
        } catch (RuntimeException error) {
            throw ScanPersistenceErrors.translate(error);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveByProjectId(UUID projectId) {
        try {
            return !entityManager.createQuery("""
                    select s.id from ScanJpaEntity s
                    where s.projectId = :projectId and s.status in (:pending, :running)
                    """, UUID.class).setParameter("projectId", projectId)
                    .setParameter("pending", ScanStatus.PENDING).setParameter("running", ScanStatus.RUNNING)
                    .setMaxResults(1).getResultList().isEmpty();
        } catch (RuntimeException error) {
            throw ScanPersistenceErrors.translate(error);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Scan> findById(UUID scanId) {
        try {
            return Optional.ofNullable(entityManager.find(ScanJpaEntity.class, scanId)).map(ScanJpaEntity::toDomain);
        } catch (RuntimeException error) {
            throw ScanPersistenceErrors.translate(error);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Scan> findByProjectIdAndId(UUID projectId, UUID scanId) {
        try {
            return entityManager.createQuery("""
                    select s from ScanJpaEntity s where s.projectId = :projectId and s.id = :scanId
                    """, ScanJpaEntity.class).setParameter("projectId", projectId).setParameter("scanId", scanId)
                    .getResultList().stream().findFirst().map(ScanJpaEntity::toDomain);
        } catch (RuntimeException error) {
            throw ScanPersistenceErrors.translate(error);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scan> findByProjectIdOrdered(UUID projectId) {
        try {
            // Orden UUID PostgreSQL sin signo, equivalente al texto canónico.
            return entityManager.createQuery("""
                    select s from ScanJpaEntity s where s.projectId = :projectId order by s.createdAt desc, s.id asc
                    """, ScanJpaEntity.class).setParameter("projectId", projectId)
                    .getResultList().stream().map(ScanJpaEntity::toDomain).toList();
        } catch (RuntimeException error) {
            throw ScanPersistenceErrors.translate(error);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scan> findActive() {
        try {
            return entityManager.createQuery("""
                    select s from ScanJpaEntity s where s.status in (:pending, :running)
                    """, ScanJpaEntity.class).setParameter("pending", ScanStatus.PENDING)
                    .setParameter("running", ScanStatus.RUNNING)
                    .getResultList().stream().map(ScanJpaEntity::toDomain).toList();
        } catch (RuntimeException error) {
            throw ScanPersistenceErrors.translate(error);
        }
    }

    @Override
    public boolean transition(ScanStatus expected, Scan scan) {
        try {
            // No se mapea ni se escribe global_score; el update condicional impide reclamos duplicados.
            return entityManager.createQuery("""
                    update ScanJpaEntity s set s.status = :status, s.startedAt = :startedAt,
                        s.finishedAt = :finishedAt, s.errorReason = :errorReason
                    where s.id = :id and s.projectId = :projectId and s.status = :expected
                    """).setParameter("status", scan.status()).setParameter("startedAt", scan.startedAt())
                    .setParameter("finishedAt", scan.finishedAt()).setParameter("errorReason", scan.errorReason())
                    .setParameter("id", scan.id()).setParameter("projectId", scan.projectId())
                    .setParameter("expected", expected).executeUpdate() == 1;
        } catch (RuntimeException error) {
            throw ScanPersistenceErrors.translate(error);
        }
    }
}
