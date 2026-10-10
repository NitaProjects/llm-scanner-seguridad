package com.llmscannerseguridad.domain.scan;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public record Scan(UUID id, UUID projectId, ScanStatus status, Instant createdAt,
        Instant startedAt, Instant finishedAt, ScanFailureReason errorReason) {

    public Scan {
        Objects.requireNonNull(id);
        Objects.requireNonNull(projectId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        for (Instant instant : new Instant[] {createdAt, startedAt, finishedAt}) {
            if (instant != null && instant.getNano() % 1000 != 0) {
                throw new IllegalArgumentException("Las fechas requieren precisión de microsegundos.");
            }
        }
        boolean dates = (startedAt == null || !startedAt.isBefore(createdAt))
                && (finishedAt == null || !finishedAt.isBefore(startedAt == null ? createdAt : startedAt));
        boolean lifecycle = switch (status) {
            case PENDING -> startedAt == null && finishedAt == null && errorReason == null;
            case RUNNING -> startedAt != null && finishedAt == null && errorReason == null;
            case COMPLETED -> startedAt != null && finishedAt != null && errorReason == null;
            case FAILED -> finishedAt != null && errorReason != null
                    && (errorReason != ScanFailureReason.SCAN_DISPATCH_FAILED || startedAt == null);
        };
        if (!dates || !lifecycle) {
            throw new IllegalArgumentException("El ciclo de vida del scan no es válido.");
        }
    }

    public static Scan pending(UUID id, UUID projectId, Instant now) {
        return new Scan(id, projectId, ScanStatus.PENDING, micros(now), null, null, null);
    }

    public Scan start(Instant now) {
        require(ScanStatus.PENDING);
        return new Scan(id, projectId, ScanStatus.RUNNING, createdAt, latest(now, createdAt), null, null);
    }

    public Scan complete(Instant now) {
        require(ScanStatus.RUNNING);
        return new Scan(id, projectId, ScanStatus.COMPLETED, createdAt, startedAt, latest(now, startedAt), null);
    }

    public Scan fail(ScanFailureReason reason, Instant now) {
        Objects.requireNonNull(reason);
        if (!status.active() || (reason == ScanFailureReason.SCAN_DISPATCH_FAILED && status != ScanStatus.PENDING)) {
            throw new IllegalStateException("La transición del scan no está permitida.");
        }
        return new Scan(id, projectId, ScanStatus.FAILED, createdAt, startedAt,
                latest(now, startedAt == null ? createdAt : startedAt), reason);
    }

    private void require(ScanStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("La transición del scan no está permitida.");
        }
    }

    private static Instant micros(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    private static Instant latest(Instant now, Instant previous) {
        Instant current = micros(now);
        return current.isBefore(previous) ? previous : current;
    }
}
