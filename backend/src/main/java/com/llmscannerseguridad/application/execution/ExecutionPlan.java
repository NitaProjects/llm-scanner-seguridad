package com.llmscannerseguridad.application.execution;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.llmscannerseguridad.domain.execution.TestExecution;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;
import com.llmscannerseguridad.domain.target.TargetConfiguration;

public record ExecutionPlan(List<SecurityTest> tests, List<TestExecution> executions,
        TargetConfiguration target, Instant scanStartedAt) {
    public ExecutionPlan {
        tests = List.copyOf(tests);
        executions = List.copyOf(executions);
        var codes = Set.of("LLM01-INSTRUCTION-001", "LLM02-CANARY-001", "LLM08-CONTEXT-001", "LLM10-MARKUP-001");
        if (target == null || scanStartedAt == null || tests.size() != 4 || executions.size() != 4
                || !tests.stream().map(SecurityTest::code).collect(java.util.stream.Collectors.toSet()).equals(codes)
                || tests.stream().anyMatch(test -> test.revision() != 1 || !test.code().startsWith(test.category().name() + "-"))
                || tests.stream().map(SecurityTest::id).distinct().count() != 4) {
            throw new IllegalStateException("El plan de pruebas no es válido.");
        }
        if (!tests.equals(tests.stream().sorted(java.util.Comparator.comparing(SecurityTest::code)
                .thenComparingInt(SecurityTest::revision)).toList())) {
            throw new IllegalStateException("El orden del plan no es válido.");
        }
        for (int i = 0; i < tests.size(); i++) {
            if (!tests.get(i).id().equals(executions.get(i).securityTestId())) {
                throw new IllegalStateException("El plan de pruebas no es válido.");
            }
        }
    }

    @Override
    public String toString() {
        return "ExecutionPlan[protegido]";
    }
}
