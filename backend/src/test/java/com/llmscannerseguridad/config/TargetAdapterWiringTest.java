package com.llmscannerseguridad.config;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.llmscannerseguridad.adapters.out.execution.securitytest.LegacyRestTargetAdapter;
import com.llmscannerseguridad.adapters.out.http.target.HttpTargetInvocationAdapter;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.reporting.*;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.application.scoring.*;
import com.llmscannerseguridad.application.target.*;
import com.llmscannerseguridad.domain.execution.TestExecution;
import com.llmscannerseguridad.domain.securitytest.*;
import com.llmscannerseguridad.domain.target.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TargetAdapterWiringTest {
    @Test
    void actualConfigurationsSelectPrimaryFakeThroughBridgeAndCloseItOnlyOnce() {
        var closes = new AtomicInteger();
        new ApplicationContextRunner()
                .withUserConfiguration(ScanConfiguration.class, SecurityTestExecutionConfiguration.class, Fakes.class)
                .withPropertyValues("app.scan.executor.core-pool-size=1", "app.scan.executor.max-pool-size=1",
                        "app.scan.executor.queue-capacity=10")
                .withBean("closeCounter", AtomicInteger.class, () -> closes)
                .withBean(EntityManager.class, () -> mock(EntityManager.class))
                .withBean(org.springframework.transaction.PlatformTransactionManager.class,
                        () -> mock(org.springframework.transaction.PlatformTransactionManager.class))
                .withBean(ProjectRepositoryPort.class, () -> mock(ProjectRepositoryPort.class))
                .withBean(TargetConfigurationRepositoryPort.class, () -> mock(TargetConfigurationRepositoryPort.class))
                .withBean(CredentialDecryptionPort.class, () -> mock(CredentialDecryptionPort.class))
                .withBean(EvaluationRepositoryPort.class, () -> mock(EvaluationRepositoryPort.class))
                .withBean(ScanScoreRepositoryPort.class, () -> mock(ScanScoreRepositoryPort.class))
                .withBean(CalculateScanScoreUseCase.class, () -> mock(CalculateScanScoreUseCase.class))
                .withBean(SecurityReportRepositoryPort.class, () -> mock(SecurityReportRepositoryPort.class))
                .withBean(GenerateReportUseCase.class, () -> mock(GenerateReportUseCase.class))
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(TargetAdapter.class);
                    assertThat(context.getBean(TargetAdapter.class)).isExactlyInstanceOf(LegacyRestTargetAdapter.class);
                    assertThat(context.getBeansOfType(HttpTargetInvocationAdapter.class)).hasSize(1);
                    var invocation = context.getBean(CountingInvocation.class);
                    assertThat(context.getBean(TargetInvocationPort.class)).isSameAs(invocation);
                    var repository = context.getBean(TestExecutionRepositoryPort.class);
                    var now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
                    var scan = UUID.randomUUID();
                    var target = new TargetConfiguration(UUID.randomUUID(), UUID.randomUUID(),
                            new TargetSettings(null, new TargetUrl("https://controlled.invalid"), "{\"m\":\"{{PROMPT}}\"}",
                                    "$.text", Map.of(), AuthType.NONE, null, 10000), null, now, now);
                    var tests = List.of("LLM01-INSTRUCTION-001", "LLM02-CANARY-001", "LLM08-CONTEXT-001", "LLM10-MARKUP-001")
                            .stream().map(code -> new SecurityTest(UUID.randomUUID(), code, 1, "fixture",
                                    SecurityCategory.valueOf(code.substring(0, 5)), "prompt-" + code)).toList();
                    var rows = tests.stream().map(test -> TestExecution.pending(UUID.randomUUID(), scan, test.id(), target.id(), now)).toList();
                    when(repository.prepare(scan)).thenReturn(new ExecutionPlan(tests, rows, target, now));
                    for (var row : rows) when(repository.start(row.id())).thenReturn(row.start(now, now));
                    when(repository.findByScanId(scan)).thenReturn(rows);
                    when(repository.allCompleted(scan)).thenReturn(true);
                    var evaluations = context.getBean(EvaluateExecutionUseCase.class);
                    when(evaluations.allCompleted(scan)).thenReturn(true);
                    context.getBean(ScanWorkPort.class).execute(scan);
                    assertThat(invocation.prompts).containsExactlyElementsOf(tests.stream().map(SecurityTest::prompt).toList());
                    assertThat(invocation.targets).allSatisfy(value -> assertThat(value).isSameAs(target));
                    assertThat(invocation.budgets).hasSize(4).allSatisfy(value -> assertThat(value).isSameAs(invocation.budgets.getFirst()));
                    verify(repository, times(4)).finish(any(), eq(new InvocationResult(200, 23L, null, "{}", "safe", false, false)));
                    verify(evaluations, times(4)).execute(any(), any(), any());
                    assertThat(closes).hasValue(0);
                });
        assertThat(closes).hasValue(1);
    }

    @Configuration(proxyBeanMethods = false)
    static class Fakes {
        @Bean @Primary CountingInvocation fixtureInvocation(AtomicInteger closes) { return new CountingInvocation(closes); }
        @Bean @Primary TestExecutionRepositoryPort fixtureExecutions() { return mock(TestExecutionRepositoryPort.class); }
        @Bean @Primary ScanLifecyclePort fixtureLifecycle() { return mock(ScanLifecyclePort.class); }
        @Bean EvaluateExecutionUseCase fixtureEvaluations() { return mock(EvaluateExecutionUseCase.class); }
    }

    static final class CountingInvocation implements TargetInvocationPort, AutoCloseable {
        private final AtomicInteger closes;
        final List<String> prompts = new ArrayList<>();
        final List<TargetConfiguration> targets = new ArrayList<>();
        final List<ScanBudget> budgets = new ArrayList<>();
        CountingInvocation(AtomicInteger closes) { this.closes = closes; }
        @Override public InvocationResult invoke(TargetConfiguration target, String prompt, ScanBudget budget) {
            targets.add(target); prompts.add(prompt); budgets.add(budget);
            return new InvocationResult(200, 23L, null, "{}", "safe", false, false);
        }
        @Override public void close() { closes.incrementAndGet(); }
    }
}
