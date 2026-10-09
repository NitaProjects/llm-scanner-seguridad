package com.llmscannerseguridad.config;

import java.time.Clock;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.llmscannerseguridad.adapters.out.crypto.target.AesGcmCredentialEncryptionAdapter;
import com.llmscannerseguridad.adapters.out.persistence.target.PostgresTargetConfigurationRepositoryAdapter;
import com.llmscannerseguridad.adapters.out.validation.target.JsonTargetConfigurationValidator;
import com.llmscannerseguridad.adapters.out.validation.target.LocalTargetUrlPolicy;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.target.ConfigurationValidationPort;
import com.llmscannerseguridad.application.target.ConfigureTargetUseCase;
import com.llmscannerseguridad.application.target.CredentialEncryptionPort;
import com.llmscannerseguridad.application.target.GetTargetConfigurationUseCase;
import com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort;
import com.llmscannerseguridad.application.target.TargetConfigurationService;
import com.llmscannerseguridad.application.target.UpdateTargetConfigurationUseCase;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class TargetConfigurationBeans {
    @Bean
    TargetConfigurationRepositoryPort targetConfigurationRepositoryPort(EntityManager entityManager) {
        return new PostgresTargetConfigurationRepositoryAdapter(entityManager);
    }

    @Bean
    ConfigurationValidationPort targetConfigurationValidationPort(Environment environment) {
        boolean enabled = Boolean.parseBoolean(environment.getProperty("TARGET_LOCAL_DEMO_ENABLED", "false"));
        String declared = environment.getProperty("TARGET_LOCAL_DEMO_ORIGINS", "");
        Set<String> origins = Arrays.stream(declared.split(",")).map(String::strip)
                .filter(value -> !value.isEmpty()).collect(Collectors.toSet());
        return new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(
                environment.acceptsProfiles(Profiles.of("local")), enabled, origins));
    }

    @Bean
    CredentialEncryptionPort targetCredentialEncryptionPort(Environment environment) {
        // Lectura diferida: GET y conservación de credencial no necesitan consultar la clave.
        return new AesGcmCredentialEncryptionAdapter(() ->
                environment.getProperty("TARGET_CREDENTIAL_ENCRYPTION_KEY"));
    }

    @Bean
    TargetConfigurationService targetConfigurationService(ProjectRepositoryPort projects,
            TargetConfigurationRepositoryPort targets, ConfigurationValidationPort validation,
            CredentialEncryptionPort encryption) {
        return new TargetConfigurationService(projects, targets, validation, encryption,
                Clock.systemUTC(), UUID::randomUUID);
    }

    @Bean
    ConfigureTargetUseCase configureTargetUseCase(TargetConfigurationService service,
            PlatformTransactionManager manager) {
        var transactions = new TargetTransactions(manager);
        return (projectId, command) -> transactions.execute(() -> service.configure(projectId, command));
    }

    @Bean
    UpdateTargetConfigurationUseCase updateTargetConfigurationUseCase(TargetConfigurationService service,
            PlatformTransactionManager manager) {
        var transactions = new TargetTransactions(manager);
        return (projectId, command) -> transactions.execute(() -> service.update(projectId, command));
    }

    @Bean
    GetTargetConfigurationUseCase getTargetConfigurationUseCase(ProjectRepositoryPort projects,
            TargetConfigurationRepositoryPort targets) {
        return new GetTargetConfigurationUseCase(projects, targets);
    }
}
