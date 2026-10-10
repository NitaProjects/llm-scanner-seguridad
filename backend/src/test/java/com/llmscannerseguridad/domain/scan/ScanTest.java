package com.llmscannerseguridad.domain.scan;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScanTest {
    private final UUID id = UUID.randomUUID();
    private final UUID project = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-10T08:00:00.123456789Z");

    @Test
    void lifecyclePreservesIdentityAndUsesNonRegressingMicroseconds() {
        var pending = Scan.pending(id, project, now);
        assertThat(pending.createdAt()).isEqualTo(Instant.parse("2026-10-10T08:00:00.123456Z"));
        assertThat(pending.status()).isEqualTo(ScanStatus.PENDING);
        assertThat(pending.startedAt()).isNull();
        assertThat(pending.finishedAt()).isNull();
        assertThat(pending.errorReason()).isNull();
        var running = pending.start(now.minusSeconds(10));
        var completed = running.complete(now.minusSeconds(20));
        assertThat(running.startedAt()).isEqualTo(pending.createdAt());
        assertThat(completed.finishedAt()).isEqualTo(running.startedAt());
        assertThat(completed.status()).isEqualTo(ScanStatus.COMPLETED);
        assertThat(completed.id()).isEqualTo(id);
        assertThat(completed.projectId()).isEqualTo(project);
        assertThat(completed.createdAt()).isEqualTo(pending.createdAt());
        assertThat(completed.errorReason()).isNull();
        var later = pending.start(now.plusSeconds(1)).complete(now.plusSeconds(2));
        assertThat(later.finishedAt()).isAfter(later.startedAt());
        assertThat(later.finishedAt().getNano() % 1000).isZero();
    }

    @Test
    void failuresBeforeAndAfterStartKeepOnlyClosedReasons() {
        var pending = Scan.pending(id, project, now);
        for (var reason : ScanFailureReason.values()) {
            var failed = pending.fail(reason, now.minusSeconds(1));
            assertThat(failed.status()).isEqualTo(ScanStatus.FAILED);
            assertThat(failed.startedAt()).isNull();
            assertThat(failed.finishedAt()).isEqualTo(pending.createdAt());
            assertThat(failed.errorReason()).isEqualTo(reason);
        }
        var running = pending.start(now.plusSeconds(1));
        for (var reason : new ScanFailureReason[] {ScanFailureReason.SCAN_PROCESSING_FAILED, ScanFailureReason.SCAN_INTERRUPTED}) {
            var failed = running.fail(reason, now);
            assertThat(failed.startedAt()).isEqualTo(running.startedAt());
            assertThat(failed.finishedAt()).isEqualTo(running.startedAt());
        }
        assertThatThrownBy(() -> running.fail(ScanFailureReason.SCAN_DISPATCH_FAILED, now))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void forbidsSkippedTransitionsReopeningAndInvalidNullCombinations() {
        var pending = Scan.pending(id, project, now);
        var running = pending.start(now);
        var completed = running.complete(now);
        var failed = pending.fail(ScanFailureReason.SCAN_INTERRUPTED, now);
        assertThatThrownBy(() -> pending.complete(now)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> running.start(now)).isInstanceOf(IllegalStateException.class);
        for (var terminal : new Scan[] {completed, failed}) {
            assertThatThrownBy(() -> terminal.start(now)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> terminal.complete(now)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> terminal.fail(ScanFailureReason.SCAN_PROCESSING_FAILED, now))
                    .isInstanceOf(IllegalStateException.class);
        }
        Instant date = pending.createdAt();
        assertThatThrownBy(() -> new Scan(id, project, ScanStatus.RUNNING, date, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Scan(id, project, ScanStatus.FAILED, date, null, date, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Scan(id, project, ScanStatus.COMPLETED, date, date, date.minusSeconds(1), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Scan(id, project, ScanStatus.PENDING, now, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Scan(id, project, ScanStatus.PENDING, date, date, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
