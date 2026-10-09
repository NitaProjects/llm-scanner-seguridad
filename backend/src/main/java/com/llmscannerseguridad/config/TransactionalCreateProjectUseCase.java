package com.llmscannerseguridad.config;

import com.llmscannerseguridad.application.project.CreateProjectCommand;
import com.llmscannerseguridad.application.project.CreateProjectUseCase;
import com.llmscannerseguridad.application.project.ProjectPersistenceException;
import com.llmscannerseguridad.domain.project.Project;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalCreateProjectUseCase implements CreateProjectUseCase {

    private final CreateProjectUseCase delegate;
    private final TransactionTemplate transaction;

    public TransactionalCreateProjectUseCase(CreateProjectUseCase delegate, PlatformTransactionManager manager) {
        this.delegate = delegate;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public Project execute(CreateProjectCommand command) {
        try {
            // execute solo retorna después del commit, incluido si existía otra transacción.
            return transaction.execute(status -> delegate.execute(command));
        } catch (TransactionException | DataAccessException error) {
            throw new ProjectPersistenceException(error);
        }
    }
}
