package com.llmscannerseguridad.config;

import java.time.Clock;
import java.util.UUID;

import com.llmscannerseguridad.adapters.out.persistence.project.PostgresProjectRepositoryAdapter;
import com.llmscannerseguridad.adapters.out.persistence.project.PostgresProjectDeletionAdapter;
import com.llmscannerseguridad.application.evaluation.EphemeralEvaluationCorpus;
import com.llmscannerseguridad.application.project.DeleteProjectUseCase;
import com.llmscannerseguridad.application.project.CreateProjectService;
import com.llmscannerseguridad.application.project.CreateProjectUseCase;
import com.llmscannerseguridad.application.project.GetProjectUseCase;
import com.llmscannerseguridad.application.project.ListProjectsUseCase;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class ProjectConfiguration {

    @Bean
    ProjectRepositoryPort projectRepositoryPort(EntityManager entityManager) {
        return new PostgresProjectRepositoryAdapter(entityManager);
    }

    @Bean
    CreateProjectUseCase createProjectUseCase(ProjectRepositoryPort repository, PlatformTransactionManager manager) {
        var service = new CreateProjectService(repository, Clock.systemUTC(), UUID::randomUUID);
        return new TransactionalCreateProjectUseCase(service, manager);
    }

    @Bean
    DeleteProjectUseCase deleteProjectUseCase(JdbcTemplate jdbc, PlatformTransactionManager manager,
            EphemeralEvaluationCorpus corpus) {
        return new DeleteProjectUseCase(new PostgresProjectDeletionAdapter(jdbc, manager, corpus));
    }

    @Bean
    GetProjectUseCase getProjectUseCase(ProjectRepositoryPort repository) {
        return new GetProjectUseCase(repository);
    }

    @Bean
    ListProjectsUseCase listProjectsUseCase(ProjectRepositoryPort repository) {
        return new ListProjectsUseCase(repository);
    }
}
