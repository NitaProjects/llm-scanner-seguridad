package com.llmscannerseguridad.config;

import java.util.function.Supplier;
import com.llmscannerseguridad.application.project.ProjectNotFoundException;
import com.llmscannerseguridad.application.target.TargetOperationException;
import com.llmscannerseguridad.domain.target.TargetValidationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class TargetTransactions {
    private final TransactionTemplate transaction;

    public TargetTransactions(PlatformTransactionManager manager) {
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T execute(Supplier<T> operation) {
        try {
            // No comunicar éxito hasta que la configuración y su credencial hayan confirmado commit.
            return transaction.execute(status -> operation.get());
        } catch (TargetOperationException | TargetValidationException | ProjectNotFoundException error) {
            throw error;
        } catch (RuntimeException error) {
            throw TargetOperationException.internal();
        }
    }
}
