package com.llmscannerseguridad.privacy;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.llmscannerseguridad.application.execution.InvocationResult;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.finding.Finding;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import tools.jackson.databind.json.JsonMapper;

class DurableEvidenceProtectionTest {
    static final String PRIVATE = "fixture-private-91";
    static final String ENCODED = Character.toString(92) + "u0066ixture-private-91";
    private final DurableEvidenceProtection guard = new DurableEvidenceProtection();

    @ParameterizedTest @ValueSource(strings = {"credential", "retrieval", "tool-argument", "tool-result", "session", "csrf", "cookie", "retired-cookie", "alias", "correlation-id"})
    void t13ToT18ProtectsEveryClassifiedOriginalAndCopiesInBothFields(String source) {
        try (var scope = ProtectionScope.open(UUID.randomUUID(), 524288)) {
            ProtectionScope.register(PRIVATE);
            var raw = new InvocationResult(200, 8L, null, JsonMapper.shared().writeValueAsString(Map.of(source, ENCODED)),
                    "prefix " + ENCODED + " suffix", false, false);
            var safe = guard.protect(raw, null);
            assertThat(safe.errorReason()).isNull();
            assertThat(safe.requestRedacted()).isTrue(); assertThat(safe.responseRedacted()).isTrue();
            assertThat(safe.responseText()).isEqualTo(ProtectedContentInspector.MARKER);
            assertThat(safe.requestBody()).contains(ProtectedContentInspector.MARKER).doesNotContain(PRIVATE, ENCODED);
            assertThat(guard.protect(safe, null)).isEqualTo(safe);
        }
        assertThat(ProtectionScope.active()).isFalse();
    }
    @Test void t19ToT24PreservesBenignBodiesAndSanitizesLateCapturesOnError() {
        try (var scope = ProtectionScope.open(UUID.randomUUID(), 524288)) {
            var raw = new InvocationResult(503, 12L, ExecutionError.HTTP_STATUS_ERROR,
                    JsonMapper.shared().writeValueAsString(Map.of("capturedLater", ENCODED)), null, false, false);
            assertThat(guard.protect(raw, null)).isEqualTo(raw);
            ProtectionScope.register(PRIVATE);
            var safe = guard.protect(raw, null);
            assertThat(safe.errorReason()).isEqualTo(ExecutionError.HTTP_STATUS_ERROR);
            assertThat(safe.httpStatus()).isEqualTo(503); assertThat(safe.latencyMs()).isEqualTo(12);
            assertThat(safe.requestBody()).doesNotContain(ENCODED, PRIVATE);
        }
    }
    @Test void uninspectableScopeAndDepthSuppressBodiesAndKeepOnlySafeMetadata() {
        try (var scope = ProtectionScope.open(UUID.randomUUID(), 524288)) {
            ProtectionScope.register(PRIVATE);
            for (String response : List.of(ProtectedContentInspectorTest.encode(PRIVATE, 5), Character.toString(92) + "u0")) {
                var safe = guard.protect(new InvocationResult(200, 10L, null, "{}", response, true, false), null);
                assertThat(safe.errorReason()).isEqualTo(ExecutionError.EVIDENCE_LIMIT_EXCEEDED);
                assertThat(safe.requestBody()).isNull(); assertThat(safe.responseText()).isNull();
                assertThat(safe.requestRedacted()).isFalse(); assertThat(safe.responseRedacted()).isFalse();
            }
            ProtectionScope.incomplete();
            assertThat(guard.protect(new InvocationResult(200, 10L, null, "{}", "SAFE", false, false), null).errorReason())
                    .isEqualTo(ExecutionError.EVIDENCE_LIMIT_EXCEEDED);
        }
    }
    @Test void registryBoundariesNeverRetainAnApparentlyCompleteSubset() {
        try (var scope = ProtectionScope.open(UUID.randomUUID(), 524288)) {
            for (int i = 0; i < 512; i++) ProtectionScope.register("synthetic-" + i);
            assertThat(ProtectionScope.complete()).isTrue(); assertThat(ProtectionScope.values()).hasSize(512);
            ProtectionScope.register("over-limit");
            assertThat(ProtectionScope.complete()).isFalse();
        }
        try (var scope = ProtectionScope.open(UUID.randomUUID(), 4)) {
            ProtectionScope.register("éé"); assertThat(ProtectionScope.complete()).isTrue();
            ProtectionScope.register("x"); assertThat(ProtectionScope.complete()).isFalse();
        }
    }
    @Test void cleanupAndConcurrentScansDoNotShareValues() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var entered = new CountDownLatch(2);
            var tasks = java.util.stream.IntStream.range(0, 2).mapToObj(i -> pool.submit(() -> {
                try (var scope = ProtectionScope.open(UUID.randomUUID(), 524288)) {
                    ProtectionScope.register("private-" + i); entered.countDown();
                    if (!entered.await(5, TimeUnit.SECONDS)) throw new IllegalStateException();
                    assertThat(ProtectionScope.values()).containsExactly("private-" + i);
                    throw new IllegalArgumentException("fixture failure");
                } catch (IllegalArgumentException expected) { assertThat(ProtectionScope.active()).isFalse(); }
                return true;
            })).toList();
            for (var future : tasks) assertThat(future.get(10, TimeUnit.SECONDS)).isTrue();
        }
    }
    @Test void t25SameSanitizedEvaluationFeedsFindingWhileRequiredSnapshotCannotBeRepaired() {
        UUID scan = UUID.randomUUID();
        try (var scope = ProtectionScope.open(scan, 524288)) {
            ProtectionScope.register(PRIVATE);
            var raw = completed(scan, new EvaluationResult.Evidence("{\"data\":\"" + ENCODED.replace("\\", "\\\\") + "\"}", true, false, 100, 100, "hash"));
            var safe = new DurableEvaluationProtection().protect(raw, 65536);
            assertThat(safe.evidence().projection()).isNull(); assertThat(safe.evidence().inputHash()).isNull();
            assertThat(safe.evidence().complete()).isFalse(); assertThat(safe.evidence().redacted()).isTrue();
            var finding = Finding.from(safe);
            assertThat(finding.description()).isEqualTo(ProtectedContentInspector.MARKER);
            assertThat(finding.evidenceSummary()).isEqualTo(ProtectedContentInspector.MARKER);
            assertThat(finding.recommendation()).isEqualTo(ProtectedContentInspector.MARKER);
            assertThat(safe.decision()).isEqualTo(raw.decision());
            assertThatThrownBy(() -> DurableEvidenceProtection.requireDocument(JsonMapper.shared().writeValueAsString(Map.of("snapshot", ENCODED)), 65536))
                    .isInstanceOf(IllegalStateException.class).hasMessage("Inspección de privacidad incompleta.");
        }
    }
    static EvaluationResult completed(UUID scan, EvaluationResult.Evidence evidence) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        var components = new HybridDecisionPolicy.ConfidenceComponents(BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        var decision = new HybridDecisionPolicy.Decision(Verdict.VULNERABLE, Verdict.VULNERABLE,
                "HYB-02", components, Severity.HIGH, true, false, false, null);
        var running = EvaluationResult.pending(UUID.randomUUID(), UUID.randomUUID(), scan, UUID.randomUUID(), SecurityCategory.LLM02, now, null)
                .start(now, now, List.of(), evidence, new EvaluationResult.LlmComponent(LlmStatus.SKIPPED, null, 0, null, null, null, null, null, null, null, null, null, null));
        return running.complete(now, decision, running.llm(), ENCODED, ENCODED, ENCODED);
    }
}
