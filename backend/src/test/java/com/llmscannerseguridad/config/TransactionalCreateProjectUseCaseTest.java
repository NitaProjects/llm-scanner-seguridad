package com.llmscannerseguridad.config;

import java.time.Instant;
import java.util.UUID;

import com.llmscannerseguridad.application.project.CreateProjectCommand;
import com.llmscannerseguridad.application.project.CreateProjectUseCase;
import com.llmscannerseguridad.application.project.ProjectPersistenceException;
import com.llmscannerseguridad.domain.project.Project;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionalCreateProjectUseCaseTest {

    @Test
    void commitsBeforeReturningAndPropagatesCommitFailure() {
        var delegate = mock(CreateProjectUseCase.class);
        var manager = mock(PlatformTransactionManager.class);
        var status = new SimpleTransactionStatus();
        var command = new CreateProjectCommand("Proyecto", null);
        var now = Instant.parse("2026-10-09T10:00:00Z");
        var project = new Project(UUID.randomUUID(), "Proyecto", null, now, now);
        when(manager.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
        when(delegate.execute(command)).thenReturn(project);
        var useCase = new TransactionalCreateProjectUseCase(delegate, manager);

        assertThat(useCase.execute(command)).isEqualTo(project);
        var order = inOrder(manager, delegate);
        order.verify(manager).getTransaction(any(TransactionDefinition.class));
        order.verify(delegate).execute(command);
        order.verify(manager).commit(status);

        doThrow(new TransactionSystemException("Fallo de commit simulado")).when(manager).commit(status);
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(ProjectPersistenceException.class);
    }
}
