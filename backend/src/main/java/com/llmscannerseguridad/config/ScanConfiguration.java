package com.llmscannerseguridad.config;

import java.time.Clock;
import java.util.UUID;
import com.llmscannerseguridad.application.execution.ExecuteSecurityTestsUseCase;
import com.llmscannerseguridad.application.execution.TestExecutionRepositoryPort;
import com.llmscannerseguridad.application.execution.TargetInvocationPort;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.adapters.out.execution.scan.SpringScanDispatcherAdapter;
import com.llmscannerseguridad.adapters.out.persistence.scan.PostgresScanRepositoryAdapter;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.scan.GetScanUseCase;
import com.llmscannerseguridad.application.scan.ListProjectScansUseCase;
import com.llmscannerseguridad.application.scan.ProcessScanUseCase;
import com.llmscannerseguridad.application.scan.RecoverInterruptedScansUseCase;
import com.llmscannerseguridad.application.scan.ScanDispatcherPort;
import com.llmscannerseguridad.application.scan.ScanLifecyclePort;
import com.llmscannerseguridad.application.scan.ScanLifecycleService;
import com.llmscannerseguridad.application.scan.ScanRepositoryPort;
import com.llmscannerseguridad.application.scan.ScanWorkPort;
import com.llmscannerseguridad.application.scan.StartScanUseCase;
import com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class ScanConfiguration {
    @Bean
    ScanExecutionGate scanExecutionGate() {
        return new ScanExecutionGate();
    }

    @Bean
    ScanRepositoryPort scanRepositoryPort(EntityManager entityManager) {
        return new PostgresScanRepositoryAdapter(entityManager);
    }

    @Bean
    ScanLifecyclePort scanLifecyclePort(ProjectRepositoryPort projects, TargetConfigurationRepositoryPort targets,
            ScanRepositoryPort scans, PlatformTransactionManager manager, ScanExecutionGate gate,
            TestExecutionRepositoryPort executions) {
        return new TransactionalScanLifecycle(new ScanLifecycleService(projects, targets, scans,
                Clock.systemUTC(), UUID::randomUUID), manager, gate, executions);
    }

    @Bean
    RecoverInterruptedScansUseCase recoverInterruptedScansUseCase(ScanLifecyclePort lifecycle) {
        return new RecoverInterruptedScansUseCase(lifecycle);
    }

    @Bean(initMethod = "initialize")
    ScanStartup scanStartup(RecoverInterruptedScansUseCase recovery, ScanExecutionGate gate) {
        // EntityManager requiere JPA y Flyway inicializados antes de recuperar.
        return new ScanStartup(recovery, gate);
    }

    @Bean
    public ScanExecutorSettings scanExecutorSettings(Environment environment) {
        try {
            return new ScanExecutorSettings(
                    environment.getRequiredProperty("app.scan.executor.core-pool-size", Integer.class),
                    environment.getRequiredProperty("app.scan.executor.max-pool-size", Integer.class),
                    environment.getRequiredProperty("app.scan.executor.queue-capacity", Integer.class));
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("La configuración del executor de scans no es válida.");
        }
    }

    @Bean
    @DependsOn("scanStartup")
    ThreadPoolTaskExecutor scanExecutor(ScanExecutorSettings settings) {
        return settings.executor();
    }

    @Bean
    ScanWorkPort scanWorkPort(TestExecutionRepositoryPort executions, TargetInvocationPort invocation, ExecutionLimits limits) {
        return new ExecuteSecurityTestsUseCase(executions, invocation, limits.scanBudgetMs(), Clock.systemUTC(), System::nanoTime);
    }

    @Bean
    ProcessScanUseCase processScanUseCase(ScanLifecyclePort lifecycle, ScanWorkPort work, ScanExecutionGate gate) {
        return new ProcessScanUseCase(lifecycle, work, gate);
    }

    @Bean
    ScanWorkerEntry scanWorkerEntry(ProcessScanUseCase process, PlatformTransactionManager manager, ScanExecutionGate gate) {
        return new ScanWorkerEntry(process, manager, gate);
    }

    @Bean
    ScanDispatcherPort scanDispatcherPort(@Qualifier("scanExecutor") ThreadPoolTaskExecutor executor, ScanWorkerEntry worker) {
        return new SpringScanDispatcherAdapter(executor, worker::execute);
    }

    @Bean
    @DependsOn("scanStartup")
    StartScanUseCase startScanUseCase(ScanLifecyclePort lifecycle, ScanDispatcherPort dispatcher, ScanExecutionGate gate) {
        return new StartScanUseCase(lifecycle, dispatcher, gate);
    }

    @Bean
    GetScanUseCase getScanUseCase(ProjectRepositoryPort projects, ScanRepositoryPort scans) {
        return new GetScanUseCase(projects, scans);
    }

    @Bean
    ListProjectScansUseCase listProjectScansUseCase(ProjectRepositoryPort projects, ScanRepositoryPort scans) {
        return new ListProjectScansUseCase(projects, scans);
    }
}
