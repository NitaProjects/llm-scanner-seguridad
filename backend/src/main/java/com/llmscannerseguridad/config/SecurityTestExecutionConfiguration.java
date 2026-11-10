package com.llmscannerseguridad.config;

import java.net.InetAddress;
import java.time.Clock;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.adapters.out.execution.securitytest.LegacyRestTargetAdapter;
import com.llmscannerseguridad.adapters.out.http.target.BoundedDnsResolver;
import com.llmscannerseguridad.adapters.out.http.target.HttpTargetInvocationAdapter;
import com.llmscannerseguridad.adapters.out.http.target.TargetDestinationPolicy;
import com.llmscannerseguridad.adapters.out.persistence.execution.PostgresSecurityTestCatalogAdapter;
import com.llmscannerseguridad.adapters.out.persistence.execution.PostgresTestExecutionRepositoryAdapter;
import com.llmscannerseguridad.adapters.out.validation.target.JsonTargetConfigurationValidator;
import com.llmscannerseguridad.adapters.out.validation.target.LocalTargetUrlPolicy;
import com.llmscannerseguridad.adapters.out.validation.target.TargetJsonProcessor;
import com.llmscannerseguridad.application.execution.ListTestExecutionsUseCase;
import com.llmscannerseguridad.application.execution.SecurityTestCatalogPort;
import com.llmscannerseguridad.application.execution.TestExecutionRepositoryPort;
import com.llmscannerseguridad.application.execution.TargetAdapter;
import com.llmscannerseguridad.application.execution.TargetInvocationPort;
import com.llmscannerseguridad.application.scan.GetScanUseCase;
import com.llmscannerseguridad.application.scan.ScanRepositoryPort;
import com.llmscannerseguridad.application.target.CredentialDecryptionPort;
import com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class SecurityTestExecutionConfiguration {
    @Bean
    ExecutionLimits executionLimits(Environment environment) {
        try {
            return new ExecutionLimits(integer(environment, "TEST_EXECUTION_MAX_TESTS", 4),
                    integer(environment, "TEST_EXECUTION_REQUEST_BYTES", 65536),
                    integer(environment, "TEST_EXECUTION_RESPONSE_BYTES", 262144),
                    integer(environment, "TEST_EXECUTION_TEXT_BYTES", 65536),
                    integer(environment, "TEST_EXECUTION_EVIDENCE_BYTES", 131072),
                    integer(environment, "TEST_EXECUTION_SCAN_EVIDENCE_BYTES", 524288),
                    integer(environment, "TEST_EXECUTION_SCAN_BUDGET_MS", 300000),
                    integer(environment, "TEST_EXECUTION_DNS_MS", 2000),
                    integer(environment, "TEST_EXECUTION_CONNECT_MS", 5000),
                    integer(environment, "TEST_EXECUTION_INTERVAL_MS", 1000));
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Los límites técnicos de ejecución no son válidos.");
        }
    }

    private static int integer(Environment environment, String key, int fallback) {
        return environment.getProperty(key, Integer.class, fallback);
    }

    private static Set<String> origins(Environment environment, String name) {
        return Arrays.stream(environment.getProperty(name, "").split(",")).map(String::strip)
                .filter(value -> !value.isEmpty()).collect(Collectors.toSet());
    }

    @Bean
    TargetDestinationPolicy targetDestinationPolicy(Environment environment) {
        return new TargetDestinationPolicy(origins(environment, "TARGET_ALLOWED_ORIGINS"),
                environment.acceptsProfiles(Profiles.of("local")),
                Boolean.parseBoolean(environment.getProperty("TARGET_LOCAL_DEMO_ENABLED", "false")),
                origins(environment, "TARGET_LOCAL_DEMO_ORIGINS"));
    }

    @Bean(destroyMethod = "close")
    BoundedDnsResolver targetDnsResolver() {
        return new BoundedDnsResolver(InetAddress::getAllByName);
    }

    @Bean(destroyMethod = "close")
    HttpTargetInvocationAdapter targetInvocationPort(TargetDestinationPolicy destinations, BoundedDnsResolver dns,
            CredentialDecryptionPort crypto, ExecutionLimits limits) {
        var validator = new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of()));
        return new HttpTargetInvocationAdapter(destinations, dns, new TargetJsonProcessor(validator), crypto, limits);
    }

    @Bean(destroyMethod = "")
    TargetAdapter targetAdapter(TargetInvocationPort invocation) {
        return new LegacyRestTargetAdapter(invocation);
    }

    @Bean
    SecurityTestCatalogPort securityTestCatalogPort(EntityManager entityManager) {
        return new PostgresSecurityTestCatalogAdapter(entityManager);
    }

    @Bean
    TestExecutionRepositoryPort testExecutionRepositoryPort(EntityManager entityManager, ScanRepositoryPort scans,
            TargetConfigurationRepositoryPort targets, SecurityTestCatalogPort catalog,
            ExecutionLimits limits, PlatformTransactionManager manager,
            com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort evaluations) {
        return new PostgresTestExecutionRepositoryAdapter(entityManager, scans, targets, catalog, limits, manager, Clock.systemUTC(), evaluations);
    }

    @Bean
    ListTestExecutionsUseCase listTestExecutionsUseCase(GetScanUseCase scans, TestExecutionRepositoryPort executions) {
        return new ListTestExecutionsUseCase(scans, executions);
    }
}
