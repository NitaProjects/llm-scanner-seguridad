package com.llmscannerseguridad.domain.execution;

import java.time.Instant;
import java.util.UUID;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SecurityTestExecutionDomainTest {
    private final Instant now = Instant.parse("2026-10-10T08:00:00.123456Z");

    @Test
    void definitionsAreImmutableAndBoundedByUtf8Bytes() {
        var id = UUID.randomUUID();
        var test = new SecurityTest(id, "LLM01-INSTRUCTION-001", 1, "Prueba", SecurityCategory.LLM01, "😀".repeat(1024));
        assertThat(test.prompt().getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(4096);
        assertThat(test.toString()).doesNotContain(test.prompt());
        assertThatThrownBy(() -> new SecurityTest(id, test.code(), 0, test.name(), test.category(), "prompt"))
                .isInstanceOf(IllegalArgumentException.class);
        for (String invalid : new String[] {"", "😀".repeat(1025), "\uD800"}) {
            assertThatThrownBy(() -> new SecurityTest(id, test.code(), 1, test.name(), test.category(), invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(SecurityCategory.values()).containsExactly(SecurityCategory.LLM01, SecurityCategory.LLM02,
                SecurityCategory.LLM08, SecurityCategory.LLM10);
    }

    @Test
    void lifecycleKeepsTechnicalSuccessSeparateFromMeaningAndClampsClock() {
        var pending = pending();
        var running = pending.start(now.minusSeconds(10), now);
        assertThat(running.startedAt()).isEqualTo(now);
        var completed = running.finish(now.minusSeconds(1), 200, 4L, null, "{}", "<script>untrusted</script>", false, false);
        assertThat(completed.status()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(completed.finishedAt()).isEqualTo(now);
        assertThatThrownBy(() -> completed.start(now, now)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> completed.finish(now, 200, 1L, null, "{}", "", false, false))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> completed.abort(now, now, ExecutionError.EXECUTION_ABORTED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(completed.toString()).doesNotContain("script");
        assertThat(running.finish(now, 500, 1L, ExecutionError.HTTP_STATUS_ERROR, "{}", null, false, false).status())
                .isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    void pendingCanOnlyFailByAbortOrInterruptionWithoutInventedAttempt() {
        for (var reason : new ExecutionError[] {ExecutionError.EXECUTION_ABORTED, ExecutionError.EXECUTION_INTERRUPTED}) {
            var failed = pending().abort(now.minusSeconds(1), now, reason);
            assertThat(failed.finishedAt()).isEqualTo(now);
            assertThat(failed.startedAt()).isNull();
            assertThat(failed.httpStatus()).isNull();
            assertThat(failed.latencyMs()).isNull();
            assertThat(failed.requestBody()).isNull();
        }
        assertThatThrownBy(() -> pending().abort(now, now, ExecutionError.TARGET_TIMEOUT))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> pending().start(now, now).finish(now, 200, -1L, null, "{}", "", false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TestExecution pending() {
        return TestExecution.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), now);
    }
}
