package com.llmscannerseguridad.adapters.out.persistence.execution;

import java.util.List;
import com.llmscannerseguridad.application.execution.SecurityTestCatalogPort;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

public class PostgresSecurityTestCatalogAdapter implements SecurityTestCatalogPort {
    private final EntityManager entityManager;

    public PostgresSecurityTestCatalogAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityTest> findAllOrdered() {
        try {
            return entityManager.createQuery("select t from SecurityTestJpaEntity t order by t.code, t.revision",
                    SecurityTestJpaEntity.class).getResultList().stream().map(SecurityTestJpaEntity::toDomain).toList();
        } catch (RuntimeException error) {
            throw ScanOperationException.internal();
        }
    }
}
